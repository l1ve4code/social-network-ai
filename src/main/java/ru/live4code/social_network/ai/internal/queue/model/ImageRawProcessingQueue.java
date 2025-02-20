package ru.live4code.social_network.ai.internal.queue.model;

import jakarta.validation.constraints.NotNull;

public record ImageRawProcessingQueue(long transactionId, long generationId, long clientId, long clientTariffId,
                                      long themeId, @NotNull String text) {
}