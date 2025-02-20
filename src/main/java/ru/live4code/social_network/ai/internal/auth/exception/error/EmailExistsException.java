package ru.live4code.social_network.ai.internal.auth.exception.error;

import ru.live4code.social_network.ai.generated.model.ClientAuthSignUpErrorEnum;
import ru.live4code.social_network.ai.internal.auth.exception.AuthRuntimeException;

public class EmailExistsException extends AuthRuntimeException {
    public EmailExistsException() {
        super(ClientAuthSignUpErrorEnum.EMAIL_EXISTS);
    }
}
