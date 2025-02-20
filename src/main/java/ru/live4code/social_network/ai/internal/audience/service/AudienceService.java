package ru.live4code.social_network.ai.internal.audience.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import ru.live4code.social_network.ai.external.exception.ActionLimitException;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatAccountInfo;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatConnectionType;
import ru.live4code.social_network.ai.external.tenchat.service.TenChatService;
import ru.live4code.social_network.ai.internal.audience.dao.AudienceDao;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.client_settings.dao.TenChatCredentialsDao;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.internal.tariffs.model.SubscribesAndUnsubscribes;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.encryption.EncryptionService;
import ru.live4code.social_network.ai.utils.environment.service.EnvironmentService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceService {

    private static final String CLIENTS_TO_PROCESS_ENV = "ten-chat-ai.audience-service.clients-to-process.batch-size";
    private static final Long CLIENTS_TO_PROCESS_BATCH_SIZE = 100L;

    private final TariffDao tariffDao;
    private final AudienceDao audienceDao;
    private final TenChatService tenChatService;
    private final ClientInfoService clientInfoService;
    private final EncryptionService encryptionService;
    private final EnvironmentService environmentService;
    private final TransactionTemplate transactionTemplate;
    private final TenChatCredentialsDao tenChatCredentialsDao;

    public void doDailySubscribesAndUnsubscribes() {

        Map<Long, SubscribesAndUnsubscribes> clientsToProcess = audienceDao.getClientsAvailableSubscribesAndUnsubscribes(
                getClientToProcessBatchSize()
        );

        long clientsToProcessAmount = clientsToProcess.size();
        if (clientsToProcessAmount == 0L) {
            log.warn("There no clients for making subscribes and unsubscribes!");
            return;
        }

        log.info("{} clients will be processed!", clientsToProcessAmount);

        log.info("Decrypting keys for API usage...");
        Set<Long> clientIds = clientsToProcess.keySet();
        Map<Long, String> encryptedAccessTokenByClientId = tenChatCredentialsDao.getClientsAccessTokens(clientIds);
        Map<Long, String> decryptedAccessTokenByClientId =
                encryptionService.decryptByClients(encryptedAccessTokenByClientId);
        Map<Long, TenChatAccountInfo> tenChatAccountInfoByClientId
                = exchangeAccessTokenForTenChatAccountInfo(decryptedAccessTokenByClientId);

        long keysSize = decryptedAccessTokenByClientId.entrySet().size();
        log.info("{} keys were successfully decrypted!", keysSize);

        log.info("Selecting users for subscriptions...");
        Map<ClientPeopleAmount, List<String>> usersToSubscribeByClientPeopleAmount =
                getUsersToSubscribeByClientPeopleAmount(
                        clientsToProcess,
                        tenChatAccountInfoByClientId
                );
        long subscribeUsersCount = usersToSubscribeByClientPeopleAmount.values().stream()
                .mapToLong(List::size)
                .sum();
        log.info("{} users successfully selected!", subscribeUsersCount);

        log.info("Selecting users for descriptions...");
        Map<ClientPeopleAmount, List<String>> usersToUnsubscribeByClientPeopleAmount =
                getUsersToUnsubscribeByClientPeopleAmount(
                        clientsToProcess,
                        tenChatAccountInfoByClientId
                );
        long unsubscribeUsersCount = usersToUnsubscribeByClientPeopleAmount.values().stream()
                .mapToLong(List::size)
                .sum();
        log.info("{} users successfully selected!", unsubscribeUsersCount);

        log.info("Subscription process was started!");
        Map<Long, AtomicInteger> successfullySubscribedCountByClientId = getSuccessfullySubscribeActionCountByClientId(
                tenChatAccountInfoByClientId,
                usersToSubscribeByClientPeopleAmount,
                true
        );
        long successfullySubscribes = successfullySubscribedCountByClientId.values().stream()
                .mapToLong(AtomicInteger::get)
                .sum();
        log.info("{} successfully client subscriptions!", successfullySubscribes);

        log.info("Description process was started!");
        Map<Long, AtomicInteger> successfullyUnsubscribedCountByClientId = getSuccessfullySubscribeActionCountByClientId(
                tenChatAccountInfoByClientId,
                usersToUnsubscribeByClientPeopleAmount,
                false
        );
        long successfullyDescribes = successfullyUnsubscribedCountByClientId.values().stream()
                .mapToLong(AtomicInteger::get)
                .sum();
        log.info("{} successfully client descriptions!", successfullyDescribes);

        log.info("Validation processing...");
        Set<Long> successfullyProcessedClientIds = getValidatedClientSubscriptionsAndDescriptions(
                clientsToProcess,
                successfullySubscribedCountByClientId,
                successfullyUnsubscribedCountByClientId
        );
        long processedClientCount = successfullyProcessedClientIds.size();
        log.info("{} clients were successfully validated!", processedClientCount);

        transactionTemplate.executeWithoutResult(__ -> {
            audienceDao.insertDoneSubscribes(successfullySubscribedCountByClientId);
            audienceDao.insertDoneUnsubscribes(successfullyUnsubscribedCountByClientId);
            audienceDao.markProcessedAt(successfullyProcessedClientIds);
        });

        long countDifference = clientsToProcessAmount - processedClientCount;
        if (countDifference > 0) {
            String errorMessage = String.format(
                    "Difference between to process and processed clients is [%s]! Please check job: %s.",
                    countDifference,
                    getClass().getSimpleName()
            );
            throw new IllegalArgumentException(errorMessage);
        }

    }

    public ResponseEntity<Void> setDailyAudienceSubscribes(long amount) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        long availableSubscribesPerDay = audienceDao.getClientAllowedSubscribesPerDay(clientId, clientTariffId);

        if (amount > availableSubscribesPerDay || amount < 0) {
            return ResponseEntity.badRequest().build();
        }

        audienceDao.setClientSubscribesPerDayForCurrentTariff(clientId, amount);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> setDailyAudienceUnsubscribes(long amount) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        long availableUnsubscribesPerDay = audienceDao.getClientAllowedUnsubscribesPerDay(clientId, clientTariffId);

        if (amount > availableUnsubscribesPerDay || amount < 0) {
            return ResponseEntity.badRequest().build();
        }

        audienceDao.setClientUnsubscribesPerDayForCurrentTariff(clientId, amount);

        return ResponseEntity.ok().build();
    }

    private Map<Long, TenChatAccountInfo> exchangeAccessTokenForTenChatAccountInfo(
            Map<Long, String> decryptedAccessTokenByClientId
    ) {
        return decryptedAccessTokenByClientId.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), tenChatService.getAccountInfo(entry.getValue())))
                .filter(pair -> {
                    if (pair.getRight() == null) {
                        log.error("{} client can't be processed!", pair.getLeft());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private Set<Long> getValidatedClientSubscriptionsAndDescriptions(
            Map<Long, SubscribesAndUnsubscribes> clientsToProcess,
            Map<Long, AtomicInteger> successfullySubscribedCountByClientId,
            Map<Long, AtomicInteger> successfullyUnsubscribedCountByClientId
    ) {
        Set<Long> successfullyProcessedClientIds = new HashSet<>();
        clientsToProcess.forEach((clientId, subscribesAndUnsubscribesCount) -> {
            var doneSubscribes = successfullySubscribedCountByClientId.getOrDefault(clientId, new AtomicInteger(0));
            var doneUnsubscribes = successfullyUnsubscribedCountByClientId.getOrDefault(clientId, new AtomicInteger(0));

            var needSubscribes = subscribesAndUnsubscribesCount.subscribesPerDay();
            var needUnsubscribes = subscribesAndUnsubscribesCount.unsubscribesPerDay();

            if (doneSubscribes.get() >= needSubscribes && doneUnsubscribes.get() >= needUnsubscribes) {
                successfullyProcessedClientIds.add(clientId);
            }
        });
        return successfullyProcessedClientIds;
    }

    private Map<Long, AtomicInteger> getSuccessfullySubscribeActionCountByClientId(
            Map<Long, TenChatAccountInfo> tenChatAccountInfoByClientId,
            Map<ClientPeopleAmount, List<String>> usersToSubscribeAction,
            boolean needSubscribe
    ) {
        Map<Long, AtomicInteger> successfullyActions = new HashMap<>();
        usersToSubscribeAction.forEach((clientPeopleAmount, userNames) -> {
            long clientId = clientPeopleAmount.clientId;
            TenChatAccountInfo tenChatAccountInfo = tenChatAccountInfoByClientId.get(clientId);
            for (var userName : userNames) {
                var processed = false;
                try {
                    processed = tenChatService.doPeopleSubscribeAction(
                            tenChatAccountInfo.sessionId(),
                            userName,
                            needSubscribe
                    );
                } catch (ActionLimitException exception) {
                    successfullyActions.put(clientId, new AtomicInteger(clientPeopleAmount.peopleAmount()));
                    break;
                }
                if (processed) {
                    successfullyActions.computeIfAbsent(clientId, v -> new AtomicInteger(0)).incrementAndGet();
                }
            }
        });
        return successfullyActions;
    }

    private Map<ClientPeopleAmount, List<String>> getUsersToSubscribeByClientPeopleAmount(
            Map<Long, SubscribesAndUnsubscribes> clientsToProcess,
            Map<Long, TenChatAccountInfo> tenChatAccountInfoByClientId
    ) {
        Map<ClientPeopleAmount, List<String>> result = new HashMap<>();
        for (var entry : tenChatAccountInfoByClientId.entrySet()) {
            Long clientId = entry.getKey();
            TenChatAccountInfo tenChatAccountInfo = entry.getValue();

            int needSubscribes = clientsToProcess.get(clientId).subscribesPerDay();
            if (needSubscribes == 0L) {
                continue;
            }

            List<String> usersToSubscribe = new ArrayList<>();
            TenChatConnectionType[] availableConnectionTypes = TenChatConnectionType.values();
            for (var connectionType : availableConnectionTypes) {
                long alreadyAddedUsers = usersToSubscribe.size();
                long difference = needSubscribes - alreadyAddedUsers;
                if (difference <= 0L) {
                    break;
                }

                List<String> foundedUsers = tenChatService.getPeopleToSubscribe(
                        tenChatAccountInfo.sessionId(),
                        connectionType,
                        difference
                );

                usersToSubscribe.addAll(foundedUsers);
            }

            result.putIfAbsent(new ClientPeopleAmount(clientId, needSubscribes), usersToSubscribe);
        }
        return result;
    }

    private Map<ClientPeopleAmount, List<String>> getUsersToUnsubscribeByClientPeopleAmount(
            Map<Long, SubscribesAndUnsubscribes> clientsToProcess,
            Map<Long, TenChatAccountInfo> tenChatAccountInfoByClientId
    ) {
        Map<ClientPeopleAmount, List<String>> result = new HashMap<>();
        for (var entry : tenChatAccountInfoByClientId.entrySet()) {
            Long clientId = entry.getKey();
            TenChatAccountInfo tenChatAccountInfo = entry.getValue();

            int needUnsubscribes = clientsToProcess.get(clientId).unsubscribesPerDay();
            if (needUnsubscribes == 0L) {
                continue;
            }

            List<String> usersToUnsubscribe = tenChatService.getFilteredSubscriptionUserNames(
                    tenChatAccountInfo.sessionId(),
                    tenChatAccountInfo.username(),
                    needUnsubscribes,
                    false
            );

            result.putIfAbsent(new ClientPeopleAmount(clientId, needUnsubscribes), usersToUnsubscribe);
        }
        return result;
    }

    private long getClientToProcessBatchSize() {
        return environmentService.getLongValueOrDefault(CLIENTS_TO_PROCESS_ENV, CLIENTS_TO_PROCESS_BATCH_SIZE);
    }

    private record ClientPeopleAmount(long clientId, int peopleAmount) {
    }

}
