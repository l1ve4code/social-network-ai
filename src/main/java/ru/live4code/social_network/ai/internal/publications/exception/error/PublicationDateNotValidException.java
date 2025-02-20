package ru.live4code.social_network.ai.internal.publications.exception.error;

import ru.live4code.social_network.ai.generated.model.EditPublicationForClientError;
import ru.live4code.social_network.ai.internal.publications.exception.PublicationRuntimeException;

public class PublicationDateNotValidException extends PublicationRuntimeException {
    public PublicationDateNotValidException() {
        super(EditPublicationForClientError.PUBLICATION_DATE_NOT_VALID);
    }
}
