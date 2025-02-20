package ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code;

import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendCodeError;
import ru.live4code.social_network.ai.internal.client_settings.exception.TenchatCodeRuntimeException;

public class TooManyCodeAttemptsException extends TenchatCodeRuntimeException {
    public TooManyCodeAttemptsException() {
        super(ClientSettingsTenchatSendCodeError.TOO_MANY_CODE_ATTEMPTS);
    }
}
