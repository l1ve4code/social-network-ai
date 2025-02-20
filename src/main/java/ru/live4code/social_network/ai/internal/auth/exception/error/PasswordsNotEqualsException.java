package ru.live4code.social_network.ai.internal.auth.exception.error;

import ru.live4code.social_network.ai.generated.model.ClientAuthSignUpErrorEnum;
import ru.live4code.social_network.ai.internal.auth.exception.AuthRuntimeException;

public class PasswordsNotEqualsException extends AuthRuntimeException {
    public PasswordsNotEqualsException() {
        super(ClientAuthSignUpErrorEnum.PASSWORDS_NOT_EQUALS);
    }
}
