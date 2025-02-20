package ru.live4code.social_network.ai.internal.client_settings.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendCodeError;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class TenchatCodeRuntimeException extends RuntimeException {

    private String message;
    private final ClientSettingsTenchatSendCodeError errorType;

}
