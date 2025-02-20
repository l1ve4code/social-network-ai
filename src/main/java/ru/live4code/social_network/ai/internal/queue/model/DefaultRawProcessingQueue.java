package ru.live4code.social_network.ai.internal.queue.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DefaultRawProcessingQueue {
    private final long transactionId;
    private final long generationId;
    private final long clientId;
    private final long clientTariffId;
}
