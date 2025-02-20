package ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code;

import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendCodeError;
import ru.live4code.social_network.ai.internal.client_settings.exception.TenchatCodeRuntimeException;

public class UnexpectedCodeResponseException extends TenchatCodeRuntimeException {
    public UnexpectedCodeResponseException() {
        super(ClientSettingsTenchatSendCodeError.UNEXPECTED_CODE_RESPONSE);
    }
}
