package ru.live4code.social_network.ai.internal.direction.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.DirectionApi;
import ru.live4code.social_network.ai.generated.model.PublicationsDirection;
import ru.live4code.social_network.ai.internal.direction.service.DirectionService;

@RestController
@RequiredArgsConstructor
public class DirectionController implements DirectionApi {

    private final DirectionService directionService;

    @Override
    public ResponseEntity<PublicationsDirection> getClientDirection() {
        return directionService.getClientDirection();
    }

    @Override
    public ResponseEntity<Void> setClientDirection(PublicationsDirection body) {
        String text = body.getText();
        return directionService.setClientDirection(text);
    }

}
