package ru.live4code.social_network.ai.internal.client_settings.exception.error.client;

import ru.live4code.social_network.ai.generated.model.ClientSettingsEditPasswordError;
import ru.live4code.social_network.ai.internal.client_settings.exception.ClientSettingsRuntimeException;

public class NewPasswordsNotEqualsException extends ClientSettingsRuntimeException {
    public NewPasswordsNotEqualsException() {
        super(ClientSettingsEditPasswordError.NEW_PASSWORDS_NOT_EQUALS);
    }
}
