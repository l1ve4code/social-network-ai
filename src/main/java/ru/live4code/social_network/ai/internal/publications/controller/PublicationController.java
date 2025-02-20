package ru.live4code.social_network.ai.internal.publications.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.PublicationsApi;
import ru.live4code.social_network.ai.generated.model.EditPublicationForClientRequest;
import ru.live4code.social_network.ai.generated.model.Publication;
import ru.live4code.social_network.ai.generated.model.PublicationsResponse;
import ru.live4code.social_network.ai.internal.publications.service.PublicationService;

@RestController
@RequiredArgsConstructor
public class PublicationController implements PublicationsApi {

    private final PublicationService publicationService;

    @Override
    public ResponseEntity<Void> autoSplitPostsByDateForClient() {
        return publicationService.autoSplitPostsByDateForClient();
    }

    @Override
    public ResponseEntity<PublicationsResponse> getPublicationsForClient() {
        return publicationService.getPublicationsForClient();
    }

    @Override
    public ResponseEntity<Publication> getPublicationForClient(Long publicationId) {
        return publicationService.getPublicationForClient(publicationId);
    }

    @Override
    public ResponseEntity<Void> editPublicationForClient(
            Long publicationId,
            EditPublicationForClientRequest editPublicationForClientRequest
    ) {
        String notParsedDate = editPublicationForClientRequest.getDate();
        String notParsedTime = editPublicationForClientRequest.getTime();
        return publicationService.editPublicationForClient(publicationId, notParsedDate, notParsedTime);
    }
}
