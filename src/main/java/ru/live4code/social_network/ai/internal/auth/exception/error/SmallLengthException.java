package ru.live4code.social_network.ai.internal.auth.exception.error;

import ru.live4code.social_network.ai.generated.model.ClientAuthSignUpErrorEnum;
import ru.live4code.social_network.ai.internal.auth.exception.AuthRuntimeException;

public class SmallLengthException extends AuthRuntimeException {
    public SmallLengthException() {
        super(ClientAuthSignUpErrorEnum.SMALL_LENGTH);
    }
}
