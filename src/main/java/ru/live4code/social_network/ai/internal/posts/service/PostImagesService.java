package ru.live4code.social_network.ai.internal.posts.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.live4code.social_network.ai.external.minio.service.MinioService;
import ru.live4code.social_network.ai.generated.model.GenerationIdResponse;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.posts.dao.PostImagesDao;
import ru.live4code.social_network.ai.internal.posts.dao.PostsDao;
import ru.live4code.social_network.ai.internal.queue.dao.ImageRawProcessingQueueDao;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class PostImagesService {

    private static final long MAX_REGENERATIONS = 3L;

    private final TariffDao tariffDao;
    private final PostsDao postsDao;
    private final PostImagesDao postImagesDao;
    private final MinioService minioService;
    private final ClientInfoService clientInfoService;
    private final ImageRawProcessingQueueDao imageRawProcessingQueueDao;

    public ResponseEntity<Resource> getClientPostImage(String imageId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        if (tariffDao.getClientActualTariffId(clientId) == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!postImagesDao.isClientPostImage(clientId, imageId)) {
            return ResponseEntity.notFound().build();
        }

        @Nullable InputStream clientImage = minioService.getPostImageByName(imageId);
        if (clientImage == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new InputStreamResource(clientImage));
    }

    public ResponseEntity<GenerationIdResponse> generateImageForSpecificPost(long postId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Long clientThemeId = postsDao.getClientPostThemeId(clientId, clientTariffId, postId);
        if (clientThemeId == null) {
            return ResponseEntity.notFound().build();
        }

        if (postsDao.isClientApprovedPostsExists(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        if (imageRawProcessingQueueDao.isGenerationStarted(clientId, clientTariffId, clientThemeId)) {
            return new ResponseEntity<>(HttpStatus.TOO_EARLY);
        }

        @Nullable Long maxGenerationId = imageRawProcessingQueueDao.getMaxGenerationId(
                clientId,
                clientTariffId,
                clientThemeId
        );
        if (maxGenerationId == null) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        if (maxGenerationId >= MAX_REGENERATIONS) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        imageRawProcessingQueueDao.insertToQueue(clientId, clientTariffId, maxGenerationId + 1L, clientThemeId);
        @Nullable Long transactionId = imageRawProcessingQueueDao.getLastAddedTransactionId(
                clientId,
                clientTariffId,
                clientThemeId
        );
        if (transactionId == null) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        return ResponseEntity.ok(new GenerationIdResponse(transactionId));
    }

    public ResponseEntity<GenerationIdResponse> getLastGenerationIdForPostImage(long postId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Long clientThemeId = postsDao.getClientPostThemeId(clientId, clientTariffId, postId);
        if (clientThemeId == null) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        @Nullable Long transactionId = imageRawProcessingQueueDao.getLastAddedTransactionId(
                clientId,
                clientTariffId,
                clientThemeId
        );
        if (transactionId == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new GenerationIdResponse(transactionId));
    }

    public ResponseEntity<Void> getClientPostImageGenerationStatus(long generationId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!imageRawProcessingQueueDao.isTransactionExists(clientId, clientTariffId, generationId)) {
            return ResponseEntity.notFound().build();
        }

        boolean isGenerating = imageRawProcessingQueueDao.isGenerating(clientId, clientTariffId, generationId);

        return isGenerating ? new ResponseEntity<>(HttpStatus.ACCEPTED) : ResponseEntity.ok().build();
    }

}
