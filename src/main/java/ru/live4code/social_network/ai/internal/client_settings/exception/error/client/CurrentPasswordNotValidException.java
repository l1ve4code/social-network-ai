package ru.live4code.social_network.ai.internal.client_settings.exception.error.client;

import ru.live4code.social_network.ai.generated.model.ClientSettingsEditPasswordError;
import ru.live4code.social_network.ai.internal.client_settings.exception.ClientSettingsRuntimeException;

public class CurrentPasswordNotValidException extends ClientSettingsRuntimeException {
    public CurrentPasswordNotValidException() {
        super(ClientSettingsEditPasswordError.CURRENT_PASSWORD_NOT_VALID);
    }
}
