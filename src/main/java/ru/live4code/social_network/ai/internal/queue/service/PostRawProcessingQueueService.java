package ru.live4code.social_network.ai.internal.queue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.transaction.support.TransactionTemplate;
import ru.live4code.social_network.ai.external.chat_gpt.service.ChatGPTService;
import ru.live4code.social_network.ai.internal.posts.dao.PostsDao;
import ru.live4code.social_network.ai.internal.posts.model.GeneratedClientPost;
import ru.live4code.social_network.ai.internal.queue.dao.ImageRawProcessingQueueDao;
import ru.live4code.social_network.ai.internal.queue.dao.PostRawProcessingQueueDao;
import ru.live4code.social_network.ai.internal.queue.model.PostRawProcessingQueue;
import ru.live4code.social_network.ai.internal.queue.model.RawImageFromRawPost;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.environment.service.EnvironmentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostRawProcessingQueueService {

    private static final String BATCH_SIZE_ENV = "ten-chat-ai.post-raw-processing-queue.batch-size";
    private static final String POOL_SIZE_ENV = "ten-chat-ai.post-raw-processing-queue.pool-size";
    private static final long SELECT_BATCH_SIZE = 10L;
    private static final int POOL_SIZE = 10;
    private static final String TEXT_PROMPT = """
            Напиши большой текст для TenChat с уникальностью 75%% и разбей его на смысловые блоки с 
            добавлением эмодзи. В начале и в конце текста добавь по одной цитате успешных в данной сфере людей. 
            В тексте необходимо раскрыть тему и объяснить все очень подробно, чтобы смог понять каждый 
            также приводите примеры, к своим тезисам, чтобы было понятнее. Завершить текст нужно открытым 
            вопросом к подписчикам, чтобы они дали свой ответ в комментариях.
            На русском языке.
            Тема: «%s»
            """.trim();

    private final PostsDao postsDao;
    private final ChatGPTService chatGPTService;
    private final EnvironmentService environmentService;
    private final TransactionTemplate transactionTemplate;
    private final PostRawProcessingQueueDao postRawProcessingQueueDao;
    private final ImageRawProcessingQueueDao imageRawProcessingQueueDao;

    public void processPostsInRawProcessingQueue() {

        var transactionsForProcessing = postRawProcessingQueueDao.getLatestTransactions(getBatchSize());

        if (transactionsForProcessing.isEmpty()) {
            log.warn("No raw posts transactions for processing!");
            return;
        }

        var transactions = transactionsForProcessing.entrySet();

        long totalTransactionsCount = transactions.size();
        log.info("{} raw posts transactions will be processed!", totalTransactionsCount);

        var executor = Executors.newFixedThreadPool(getPoolSize());

        var completableFutures = transactions.stream()
                .map(transaction -> CompletableFuture.supplyAsync(() -> doGeneration(
                        transaction.getKey(),
                        transaction.getValue()
                ), executor))
                .toList();

        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .exceptionally(exception -> {
                    log.error("Exception was caught while building async posts generation: {}!", exception.getMessage());
                    return null;
                })
                .join();

        executor.shutdown();

        var result = completableFutures.stream()
                .filter(item -> !item.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .toList();

        List<Long> transactionsForMarkProcessed = result.stream().map(Pair::getLeft).toList();
        List<GeneratedClientPost> postsForInsert = result.stream().flatMap(Pair::getRight).toList();
        List<RawImageFromRawPost> imagesForRawQueue = postsForInsert.stream()
                .map(RawImageFromRawPost::fromGeneratedClientPost)
                .toList();

        var processedTransactionCount = transactionsForMarkProcessed.size();
        if (processedTransactionCount == 0L) {
            throw new IllegalStateException("There no transactions for insert, but they were!");
        }

        transactionTemplate.executeWithoutResult(__ -> {
            postRawProcessingQueueDao.markProcessed(transactionsForMarkProcessed);
            postsDao.insertGeneratedPosts(postsForInsert);
            imageRawProcessingQueueDao.insertToQueueWithDefaultGenerationId(imagesForRawQueue);
        });

        log.info("{} transactions were marked as processed!", processedTransactionCount);
        log.info("{} rows were inserted to social_network.client_posts!", postsForInsert.size());

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

    private Pair<Long, Stream<GeneratedClientPost>> doGeneration(
            long transactionId,
            List<PostRawProcessingQueue> transactions
    ) {
        var clientPosts = transactions.stream().map(transaction -> {

            var clientTheme = transaction.themeText();
            var neuralPrompt = TEXT_PROMPT.formatted(clientTheme);

            var textNeuralNetworkResponse = chatGPTService.getChatGPTAnswer(neuralPrompt);

            return new GeneratedClientPost(
                    transaction.clientId(),
                    transaction.clientTariffId(),
                    transaction.themeId(),
                    textNeuralNetworkResponse
            );
        });

        return Pair.of(transactionId, clientPosts);
    }

    private long getBatchSize() {
        return environmentService.getLongValueOrDefault(BATCH_SIZE_ENV, SELECT_BATCH_SIZE);
    }

    private int getPoolSize() {
        return environmentService.getIntValueOrDefault(POOL_SIZE_ENV, POOL_SIZE);
    }

}
