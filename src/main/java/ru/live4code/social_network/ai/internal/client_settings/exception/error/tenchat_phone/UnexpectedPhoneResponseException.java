package ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone;

import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendPhoneError;
import ru.live4code.social_network.ai.internal.client_settings.exception.TenchatPhoneRuntimeException;

public class UnexpectedPhoneResponseException extends TenchatPhoneRuntimeException {
    public UnexpectedPhoneResponseException() {
        super(ClientSettingsTenchatSendPhoneError.UNEXPECTED_PHONE_RESPONSE);
    }
}
