package ru.live4code.social_network.ai.internal.auth.exception.error;

import ru.live4code.social_network.ai.generated.model.ClientAuthSignUpErrorEnum;
import ru.live4code.social_network.ai.internal.auth.exception.AuthRuntimeException;

public class NotCreatedException extends AuthRuntimeException {
    public NotCreatedException() {
        super(ClientAuthSignUpErrorEnum.NOT_CREATED);
    }
}
