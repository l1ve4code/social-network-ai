package ru.live4code.social_network.ai.internal.publications.exception.error;

import ru.live4code.social_network.ai.generated.model.EditPublicationForClientError;
import ru.live4code.social_network.ai.internal.publications.exception.PublicationRuntimeException;

public class PastPublicationForbiddenException extends PublicationRuntimeException {
    public PastPublicationForbiddenException( ) {
        super(EditPublicationForClientError.PAST_PUBLICATION_FORBIDDEN);
    }
}
