package ru.live4code.social_network.ai.internal.queue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.transaction.support.TransactionTemplate;
import ru.live4code.social_network.ai.external.chat_gpt.service.ChatGPTService;
import ru.live4code.social_network.ai.internal.queue.dao.ThemeRawProcessingQueueDao;
import ru.live4code.social_network.ai.internal.queue.model.ThemeRawProcessingQueue;
import ru.live4code.social_network.ai.internal.themes.dao.ThemesDao;
import ru.live4code.social_network.ai.internal.themes.model.GeneratedClientTheme;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.environment.service.EnvironmentService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThemeRawProcessingQueueService {

    private static final String BATCH_SIZE_ENV = "ten-chat-ai.theme-raw-processing-queue.batch-size";
    private static final String POOL_SIZE_ENV = "ten-chat-ai.theme-raw-processing-queue.pool-size";
    private static final long SELECT_BATCH_SIZE = 10L;
    private static final int POOL_SIZE = 10;
    private static final String TEXT_PROMPT = """
            Напиши %s тем постов для компании, которая занимается %s, там должно быть как и развеивание мифов,
            так и интересные истории и необычные кейсы (такие например, как мы однажды конфисковывали за неоплату
            машину в чечне со двора Кадырова). Перед темой поста указать его номер, после номера точка.
            """.trim();

    private final ThemesDao themesDao;
    private final ChatGPTService chatGPTService;
    private final EnvironmentService environmentService;
    private final TransactionTemplate transactionTemplate;
    private final ThemeRawProcessingQueueDao themeRawProcessingQueueDao;

    public void processThemesInRawProcessingQueue() {

        var transactionsForProcessing = themeRawProcessingQueueDao.getLatestTransactions(getBatchSize());

        if (transactionsForProcessing.isEmpty()) {
            log.warn("No raw themes transactions for processing!");
            return;
        }

        long totalTransactionsCount = transactionsForProcessing.size();
        log.info("{} raw themes transactions will be processed!", totalTransactionsCount);

        var executor = Executors.newFixedThreadPool(getPoolSize());

        var completableFutures = transactionsForProcessing.stream()
                .map(transaction -> CompletableFuture.supplyAsync(() -> doGeneration(transaction), executor))
                .toList();

        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new))
                .exceptionally(exception -> {
                    log.error("Exception was caught while building async theme generation: {}!", exception.getMessage());
                    return null;
                })
                .join();

        executor.shutdown();

        var result = completableFutures.stream()
                .filter(item -> !item.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .toList();

        List<Long> transactionsForMarkProcessed = result.stream().map(Pair::getLeft).toList();
        List<GeneratedClientTheme> themesForInsert = result.stream().flatMap(Pair::getRight).toList();

        var processedTransactionCount = transactionsForMarkProcessed.size();
        if (processedTransactionCount == 0L) {
            throw new IllegalStateException("There no transactions for insert, but they were!");
        }

        transactionTemplate.executeWithoutResult(__ -> {
            themeRawProcessingQueueDao.markProcessed(transactionsForMarkProcessed);
            themesDao.insertGeneratedThemes(themesForInsert);
        });

        log.info("{} transactions were marked as processed!", processedTransactionCount);
        log.info("{} rows were inserted to social_network.client_themes!", themesForInsert.size());

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

    private Pair<Long, Stream<GeneratedClientTheme>> doGeneration(ThemeRawProcessingQueue transaction) {

        String neuralPrompt = TEXT_PROMPT.formatted(transaction.getThemesAmount(), transaction.getDirectionText());

        String neuralNetworkResponse = chatGPTService.getChatGPTAnswer(neuralPrompt);

        var clientThemes = Arrays.stream(neuralNetworkResponse.split("\n"))
                .filter(StringUtils::isNotEmpty)
                .map(theme -> new GeneratedClientTheme(
                        transaction.getGenerationId(),
                        transaction.getClientId(),
                        transaction.getClientTariffId(),
                        transaction.getDirectionId(),
                        theme
                ));

        return Pair.of(transaction.getTransactionId(), clientThemes);
    }

    private long getBatchSize() {
        return environmentService.getLongValueOrDefault(BATCH_SIZE_ENV, SELECT_BATCH_SIZE);
    }

    private int getPoolSize() {
        return environmentService.getIntValueOrDefault(POOL_SIZE_ENV, POOL_SIZE);
    }

}
