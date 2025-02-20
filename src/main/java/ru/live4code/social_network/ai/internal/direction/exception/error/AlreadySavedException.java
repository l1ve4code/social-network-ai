package ru.live4code.social_network.ai.internal.direction.exception.error;

import ru.live4code.social_network.ai.generated.model.PublicationsDirectionError;
import ru.live4code.social_network.ai.internal.direction.exception.DirectionRuntimeException;

public class AlreadySavedException extends DirectionRuntimeException {
    public AlreadySavedException() {
        super(PublicationsDirectionError.ALREADY_SAVED);
    }
}
