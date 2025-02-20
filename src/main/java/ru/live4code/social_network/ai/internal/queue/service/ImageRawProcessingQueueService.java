package ru.live4code.social_network.ai.internal.queue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.transaction.support.TransactionTemplate;
import ru.live4code.social_network.ai.external.image_gpt.service.KandinskyGPTService;
import ru.live4code.social_network.ai.external.minio.service.MinioService;
import ru.live4code.social_network.ai.internal.posts.dao.PostImagesDao;
import ru.live4code.social_network.ai.internal.posts.model.GeneratedClientPostImage;
import ru.live4code.social_network.ai.internal.queue.dao.ImageRawProcessingQueueDao;
import ru.live4code.social_network.ai.internal.queue.model.ImageRawProcessingQueue;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.environment.service.EnvironmentService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageRawProcessingQueueService {

    private static final String BATCH_SIZE_ENV = "ten-chat-ai.image-raw-processing-queue.batch-size";
    private static final String PARTITION_SIZE_ENV = "ten-chat-ai.image-raw-processing-queue.partition-size";
    private static final String POOL_SIZE_ENV = "ten-chat-ai.image-raw-processing-queue.pool-size";
    private static final long SELECT_BATCH_SIZE = 300L;
    private static final int PARTITION_BATCH_SIZE = 10;
    private static final int POOL_SIZE = 10;

    private final MinioService minioService;
    private final PostImagesDao postImagesDao;
    private final EnvironmentService environmentService;
    private final KandinskyGPTService kandinskyGPTService;
    private final TransactionTemplate transactionTemplate;
    private final ImageRawProcessingQueueDao imageRawProcessingQueueDao;

    public void processImagesInRawProcessingQueue() {

        if (!minioService.isPostImagesStorageExists()) {
            log.warn("S3 for storing post images not exists /or unavailable!");
            return;
        }

        var transactionsForProcessing = imageRawProcessingQueueDao.getLatestTransactions(getBatchSize());

        if (transactionsForProcessing.isEmpty()) {
            log.warn("No raw images transactions for processing!");
            return;
        }

        long totalTransactionsCount = transactionsForProcessing.size();
        log.info("{} raw images transactions will be processed!", totalTransactionsCount);

        var partitionedTransactions = ListUtils.partition(transactionsForProcessing, getPartitionSize());
        log.info("{} raw images batches will be processed!", partitionedTransactions.size());

        var executor = Executors.newFixedThreadPool(getPoolSize());

        var completableFutures = partitionedTransactions.stream()
                .map(transaction -> CompletableFuture.supplyAsync(() -> doGeneration(transaction), executor))
                .toList();

        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .exceptionally(exception -> {
                    log.error("Exception was caught while building async images generation: {}!", exception.getMessage());
                    return null;
                })
                .join();

        executor.shutdown();

        var batchClientImagesForProcess = completableFutures.stream()
                .filter(item -> !item.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .toList();

        var processedTransactionCount = batchClientImagesForProcess.stream().mapToInt(List::size).sum();
        if (processedTransactionCount == 0L) {
            throw new IllegalStateException("There no transactions for insert, but they were!");
        }

        for (var batch : batchClientImagesForProcess) {
            var transactionIds = batch.stream().map(GeneratedClientPostImage::transactionId).toList();
            var imagesForUpload = batch.stream().map(GeneratedClientPostImage::toFilenameBytearray).toList();
            var imagesForInsert = batch.stream().map(GeneratedClientPostImage::toGeneratedClientImage).toList();

            var isUploaded = minioService.uploadPostImages(imagesForUpload);

            if (!isUploaded) {
                log.error("Can't upload client images to S3! Please check job: {}!", getClass().getSimpleName());
                continue;
            }

            transactionTemplate.executeWithoutResult(__ -> {
                imageRawProcessingQueueDao.markProcessed(transactionIds);
                postImagesDao.insertGeneratedImages(imagesForInsert);
            });

            log.info("{} transactions were marked as processed!", transactionIds.size());
            log.info("{} rows were inserted to social_network.client_post_images!", imagesForInsert.size());
        }

        var transactionDiff = totalTransactionsCount - processedTransactionCount;
        if (transactionDiff > 0L) {
            String errorMessage = String.format(
                    "Difference between to process and processed transactions is [%s]! Please check job: %s.",
                    transactionDiff,
                    getClass().getSimpleName()
            );
            throw new IllegalStateException(errorMessage);
        }

    }

    private List<GeneratedClientPostImage> doGeneration(List<ImageRawProcessingQueue> transactions) {
        return transactions.stream().map(transaction -> {
            long clientId = transaction.clientId();

            var imageNeuralNetworkResponse = kandinskyGPTService.getImageByPrompt(transaction.text());

            byte[] decodedImage = Base64.decode(imageNeuralNetworkResponse);
            String imageId = String.format("%s%s", clientId, UUID.randomUUID());

            return new GeneratedClientPostImage(
                    transaction.transactionId(),
                    transaction.generationId(),
                    clientId,
                    transaction.clientTariffId(),
                    transaction.themeId(),
                    imageId,
                    decodedImage
            );
        }).toList();
    }

    private long getBatchSize() {
        return environmentService.getLongValueOrDefault(BATCH_SIZE_ENV, SELECT_BATCH_SIZE);
    }

    private int getPartitionSize() {
        return environmentService.getIntValueOrDefault(PARTITION_SIZE_ENV, PARTITION_BATCH_SIZE);
    }

    private int getPoolSize() {
        return environmentService.getIntValueOrDefault(POOL_SIZE_ENV, POOL_SIZE);
    }

}
