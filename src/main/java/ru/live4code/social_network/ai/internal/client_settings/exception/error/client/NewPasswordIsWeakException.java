package ru.live4code.social_network.ai.internal.client_settings.exception.error.client;

import ru.live4code.social_network.ai.generated.model.ClientSettingsEditPasswordError;
import ru.live4code.social_network.ai.internal.client_settings.exception.ClientSettingsRuntimeException;

public class NewPasswordIsWeakException extends ClientSettingsRuntimeException {
    public NewPasswordIsWeakException() {
        super(ClientSettingsEditPasswordError.NEW_PASSWORD_IS_WEAK);
    }
}
