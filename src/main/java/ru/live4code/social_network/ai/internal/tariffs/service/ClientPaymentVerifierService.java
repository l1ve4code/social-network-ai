package ru.live4code.social_network.ai.internal.tariffs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import ru.live4code.social_network.ai.external.yoomoney.model.PaymentStatus;
import ru.live4code.social_network.ai.external.yoomoney.service.YoomoneyService;
import ru.live4code.social_network.ai.internal.tariffs.dao.ClientPaymentDao;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.internal.tariffs.model.ClientTariff;
import ru.live4code.social_network.ai.internal.tariffs.model.ClientTariffPayment;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.environment.service.EnvironmentService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientPaymentVerifierService {

    private static final String BATCH_SIZE_ENV = "ten-chat-ai.client-payment-verifier.batch-size";
    private static final long SELECT_BATCH_SIZE = 100L;

    private final TariffDao tariffDao;
    private final YoomoneyService yoomoneyService;
    private final ClientPaymentDao clientPaymentDao;
    private final EnvironmentService environmentService;
    private final TransactionTemplate transactionTemplate;

    public void verifyClientPayments() {

        List<ClientTariffPayment> paymentsToValidate = clientPaymentDao.getClientPaymentsToApprove(getSelectBatchSize());
        long paymentsSize = paymentsToValidate.size();

        if (paymentsSize == 0L) {
            log.warn("There no row(s) for validate!");
            return;
        }

        log.info("{} payment(s) will be processed!", paymentsSize);

        Set<String> errorPaymentIds = new HashSet<>();
        Set<ClientTariffPayment> validatedPayments = new HashSet<>();
        paymentsToValidate.forEach(payment -> {
            String paymentId = payment.paymentId();
            PaymentStatus paymentStatus = yoomoneyService.getPaymentStatus(paymentId);
            switch (paymentStatus) {
                case SUCCEEDED -> validatedPayments.add(payment);
                case ERROR -> errorPaymentIds.add(paymentId);
            }
        });

        if (!errorPaymentIds.isEmpty()) {
            log.error("There {} payment(s) with errors!", errorPaymentIds.size());
        }

        Set<String> validatedPaymentIds = validatedPayments.stream()
                .map(ClientTariffPayment::paymentId)
                .collect(Collectors.toSet());
        Set<ClientTariff> validatedClientTariffs = validatedPayments.stream()
                .map(payment -> new ClientTariff(payment.clientId(), payment.tariffId()))
                .collect(Collectors.toSet());

        log.info("Total validated payments id(s): {}.", validatedPaymentIds.size());

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusMonths(1L);

        transactionTemplate.executeWithoutResult(__ -> {
            tariffDao.insertClientTariffs(validatedClientTariffs, from, to);
            clientPaymentDao.markPaymentsSucceeded(validatedPaymentIds);
            clientPaymentDao.markPaymentsError(errorPaymentIds);
        });

        log.info("{} tariff(s) were given to client(s)!", validatedClientTariffs.size());

    }

    private long getSelectBatchSize() {
        return environmentService.getLongValueOrDefault(BATCH_SIZE_ENV, SELECT_BATCH_SIZE);
    }

}
