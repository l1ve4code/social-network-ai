package ru.live4code.social_network.ai.internal.queue.model;

import jakarta.validation.constraints.NotNull;

public record PostRawProcessingQueue(long transactionId, long clientId, long clientTariffId, long themeId,
                                     @NotNull String themeText) {
}
