package ru.live4code.social_network.ai.internal.publications.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.live4code.social_network.ai.generated.model.EditPublicationForClientError;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class PublicationRuntimeException extends RuntimeException {

    private String message;
    private final EditPublicationForClientError errorType;

}
