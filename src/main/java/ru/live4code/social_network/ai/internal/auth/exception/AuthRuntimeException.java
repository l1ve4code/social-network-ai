package ru.live4code.social_network.ai.internal.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.live4code.social_network.ai.generated.model.ClientAuthSignUpErrorEnum;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class AuthRuntimeException extends RuntimeException {

    private String message;
    private final ClientAuthSignUpErrorEnum errorType;

}
