package ru.live4code.social_network.ai.internal.direction.exception.error;

import ru.live4code.social_network.ai.generated.model.PublicationsDirectionError;
import ru.live4code.social_network.ai.internal.direction.exception.DirectionRuntimeException;

public class TooLowWordsException extends DirectionRuntimeException {
    public TooLowWordsException() {
        super(PublicationsDirectionError.TOO_LOW_WORDS);
    }
}
