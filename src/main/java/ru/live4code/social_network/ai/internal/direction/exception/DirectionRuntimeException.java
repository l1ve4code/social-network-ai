package ru.live4code.social_network.ai.internal.direction.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.live4code.social_network.ai.generated.model.PublicationsDirectionError;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class DirectionRuntimeException extends RuntimeException {

    private String message;
    private final PublicationsDirectionError errorType;

}
