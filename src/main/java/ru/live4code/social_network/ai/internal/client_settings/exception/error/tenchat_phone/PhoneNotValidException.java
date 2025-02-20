package ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone;

import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendPhoneError;
import ru.live4code.social_network.ai.internal.client_settings.exception.TenchatPhoneRuntimeException;

public class PhoneNotValidException extends TenchatPhoneRuntimeException {
    public PhoneNotValidException() {
        super(ClientSettingsTenchatSendPhoneError.PHONE_NOT_VALID);
    }
}
