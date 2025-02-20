package ru.live4code.social_network.ai.internal.queue.model;

import lombok.Getter;

@Getter
public class ThemeRawProcessingQueue extends DefaultRawProcessingQueue {

    private final long themesAmount;
    private final long directionId;
    private final String directionText;

    public ThemeRawProcessingQueue(
            long transactionId,
            long generationId,
            long clientId,
            long clientTariffId,
            long directionId,
            String directionText,
            long themesAmount
    ) {
        super(transactionId, generationId, clientId, clientTariffId);
        this.directionId = directionId;
        this.directionText = directionText;
        this.themesAmount = themesAmount;
    }

}
