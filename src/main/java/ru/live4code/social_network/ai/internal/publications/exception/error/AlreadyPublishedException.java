package ru.live4code.social_network.ai.internal.publications.exception.error;

import ru.live4code.social_network.ai.generated.model.EditPublicationForClientError;
import ru.live4code.social_network.ai.internal.publications.exception.PublicationRuntimeException;

public class AlreadyPublishedException extends PublicationRuntimeException {
    public AlreadyPublishedException() {
        super(EditPublicationForClientError.ALREADY_PUBLISHED);
    }
}
