package ru.live4code.social_network.ai.internal.publications.exception.error;

import ru.live4code.social_network.ai.generated.model.EditPublicationForClientError;
import ru.live4code.social_network.ai.internal.publications.exception.PublicationRuntimeException;

public class PublicationTimeNotValidException extends PublicationRuntimeException {
    public PublicationTimeNotValidException() {
        super(EditPublicationForClientError.PUBLICATION_TIME_NOT_VALID);
    }
}
