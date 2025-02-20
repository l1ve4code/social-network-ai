package ru.live4code.social_network.ai.internal.publications.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.live4code.social_network.ai.external.minio.service.MinioService;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatAccountInfo;
import ru.live4code.social_network.ai.external.tenchat.service.TenChatService;
import ru.live4code.social_network.ai.generated.model.Publication;
import ru.live4code.social_network.ai.generated.model.PublicationsResponse;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.client_settings.dao.TenChatCredentialsDao;
import ru.live4code.social_network.ai.internal.posts.dao.PostsDao;
import ru.live4code.social_network.ai.internal.publications.dao.PublicationDao;
import ru.live4code.social_network.ai.internal.publications.exception.PublicationRuntimeException;
import ru.live4code.social_network.ai.internal.publications.exception.error.*;
import ru.live4code.social_network.ai.internal.publications.model.PostPublishAt;
import ru.live4code.social_network.ai.internal.publications.model.PublicationWOImage;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.internal.tariffs.model.TariffRange;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.encryption.EncryptionService;

import java.io.IOException;
import java.io.InputStream;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicationService {

    private static final LocalTime PUBLICATION_FROM_TIME = LocalTime.of(10, 0);
    private static final LocalTime PUBLICATION_TO_TIME = LocalTime.of(19, 0);

    private final PostsDao postsDao;
    private final TariffDao tariffDao;
    private final MinioService minioService;
    private final PublicationDao publicationDao;
    private final TenChatService tenChatService;
    private final EncryptionService encryptionService;
    private final TenChatCredentialsDao tenChatCredentialsDao;
    private final ClientInfoService clientInfoService;

    public void publishClientPosts() {

        if (!minioService.isPostImagesStorageExists()) {
            log.error("S3 for storing post images not exists /or unavailable!");
            return;
        }

        List<PublicationWOImage> publicationsWOImage = publicationDao.getPostsForPublish();

        if (publicationsWOImage.isEmpty()) {
            log.warn("There no posts for publish!");
            return;
        }

        Set<Long> clientIds = publicationsWOImage.stream().map(PublicationWOImage::clientId).collect(Collectors.toSet());
        Map<Long, String> encryptedAccessTokenByClientId = tenChatCredentialsDao.getClientsAccessTokens(clientIds);
        Map<Long, String> decryptedAccessTokenByClientId =
                encryptionService.decryptByClients(encryptedAccessTokenByClientId);
        Map<Long, TenChatAccountInfo> clientAccessCredentials =
                exchangeAccessTokenForTenChatAccountInfo(decryptedAccessTokenByClientId);

        long publicationSize = publicationsWOImage.size();
        log.info("{} posts will be published!", publicationSize);

        var processedIds = new HashSet<Long>();
        for (var publication : publicationsWOImage) {

            long clientId = publication.clientId();

            if (!clientAccessCredentials.containsKey(clientId)) {
                log.error("{} client can't be processed! Error while getting account info.", clientId);
                continue;
            }

            @Nullable InputStream imageInputStream = minioService.getPostImageByName(publication.imageId());
            if (imageInputStream == null) {
                log.error("Can't get image for {} client!", clientId);
                continue;
            }

            byte[] imageBytes;
            try {
                imageBytes = imageInputStream.readAllBytes();
            } catch (IOException exception) {
                log.error(exception.getMessage());
                continue;
            }

            var published = tenChatService.publishPost(
                    clientAccessCredentials.get(clientId).sessionId(),
                    publication.title(),
                    publication.description(),
                    imageBytes
            );

            if (!published) {
                log.error("Post for {} client wasn't published!", clientId);
                continue;
            }

            processedIds.add(publication.id());

        }

        publicationDao.markPublished(processedIds);

        long publishedSize = processedIds.size();
        log.info("{} rows were marked as processed!", publishedSize);

        long sizeDifference = publicationSize - publishedSize;
        if (sizeDifference > 0) {
            var errorMessage = String.format(
                    "%s rows weren't published! Please check job: %s",
                    sizeDifference,
                    getClass().getSimpleName()
            );
            throw new IllegalStateException(errorMessage);
        }

    }

    public ResponseEntity<Void> autoSplitPostsByDateForClient() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        List<Long> approvedPostIds = postsDao.getApprovedPostIdsForClient(clientId, clientTariffId);
        if (approvedPostIds.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (publicationDao.isClientPublicationsExists(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        TariffRange tariffRange = tariffDao.getClientActualTariffDateRangeByTariffId(clientId, clientTariffId);
        LocalDate tomorrow = LocalDate.now().plusDays(1L);
        LocalDate toTariff = tariffRange.end();

        long daysDifference = ChronoUnit.DAYS.between(tomorrow, toTariff);
        long minutesDifference = ChronoUnit.MINUTES.between(PUBLICATION_FROM_TIME, PUBLICATION_TO_TIME);

        List<PostPublishAt> publications = approvedPostIds.stream().map(id -> {

            var publicationDate = tomorrow.plusDays(ThreadLocalRandom.current().nextLong(daysDifference + 1));
            var publicationTime = PUBLICATION_FROM_TIME.plusMinutes(
                    ThreadLocalRandom.current().nextLong(minutesDifference + 1)
            );
            var publishAt = OffsetDateTime.of(publicationDate, publicationTime, ZoneOffset.ofHours(3));

            return new PostPublishAt(id, publishAt);

        }).toList();

        publicationDao.insertPublications(clientId, clientTariffId, publications);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<PublicationsResponse> getPublicationsForClient() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        List<Publication> clientPublications = publicationDao.getClientPublications(clientId, clientTariffId);
        if (clientPublications.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var response = new PublicationsResponse(clientPublications);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Publication> getPublicationForClient(long publicationId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Publication clientPublication = publicationDao.getClientPublication(
                clientId,
                clientTariffId,
                publicationId
        );
        if (clientPublication == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(clientPublication);
    }

    public ResponseEntity<Void> editPublicationForClient(
            long publicationId,
            String notParsedDate,
            String notParsedTime
    ) {
        LocalDate newTariffDate =
                parseTemporalOrThrow(notParsedDate, LocalDate::parse, new PublicationDateNotValidException());
        LocalTime newTariffTime =
                parseTemporalOrThrow(notParsedTime, LocalTime::parse, new PublicationTimeNotValidException());

        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!publicationDao.isClientPublicationExists(clientId, clientTariffId, publicationId)) {
            return ResponseEntity.notFound().build();
        }

        TariffRange tariffRange = tariffDao.getClientActualTariffDateRangeByTariffId(clientId, clientTariffId);
        LocalDate tariffStart = tariffRange.start();
        LocalDate tariffEnd = tariffRange.end();

        if (newTariffDate.isAfter(tariffEnd) || newTariffDate.isBefore(tariffStart)) {
            throw new PublicationDateOutOfTariffBordersException();
        }

        OffsetDateTime now = OffsetDateTime.now().plusMinutes(15L);
        var newPublicationTime = OffsetDateTime.of(newTariffDate, newTariffTime, ZoneOffset.ofHours(3));
        if (newPublicationTime.isBefore(now)) {
            throw new PastPublicationForbiddenException();
        }

        if (publicationDao.isPublished(clientId, clientTariffId, publicationId)) {
            throw new AlreadyPublishedException();
        }

        publicationDao.changeClientPublicationPublishTime(clientId, clientTariffId, publicationId, newPublicationTime);

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

    private static <V extends Temporal, T extends PublicationRuntimeException> V parseTemporalOrThrow(
            String date,
            Function<String, V> parse,
            T exception
    ) {
        try {
            return parse.apply(date);
        } catch (DateTimeException e) {
            throw exception;
        }
    }

}
