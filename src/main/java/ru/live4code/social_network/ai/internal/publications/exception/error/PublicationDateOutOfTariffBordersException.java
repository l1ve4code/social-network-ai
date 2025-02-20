package ru.live4code.social_network.ai.internal.publications.exception.error;

import ru.live4code.social_network.ai.generated.model.EditPublicationForClientError;
import ru.live4code.social_network.ai.internal.publications.exception.PublicationRuntimeException;

public class PublicationDateOutOfTariffBordersException extends PublicationRuntimeException {
    public PublicationDateOutOfTariffBordersException() {
        super(EditPublicationForClientError.PUBLICATION_DATE_OUT_OF_TARIFF_BORDERS);
    }
}
