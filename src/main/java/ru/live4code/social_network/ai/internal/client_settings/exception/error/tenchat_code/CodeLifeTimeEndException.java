package ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code;

import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendCodeError;
import ru.live4code.social_network.ai.internal.client_settings.exception.TenchatCodeRuntimeException;

public class CodeLifeTimeEndException extends TenchatCodeRuntimeException {
    public CodeLifeTimeEndException() {
        super(ClientSettingsTenchatSendCodeError.CODE_LIFE_TIME_END);
    }
}
