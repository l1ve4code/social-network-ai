package ru.live4code.social_network.ai.internal.audience.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.AudienceApi;
import ru.live4code.social_network.ai.generated.model.SetDailyAudienceSubscribesAndUnsubscribesRequest;
import ru.live4code.social_network.ai.internal.audience.service.AudienceService;

@RestController
@RequiredArgsConstructor
public class AudienceController implements AudienceApi {

    private final AudienceService audienceService;

    @Override
    public ResponseEntity<Void> setDailyAudienceSubscribes(
            SetDailyAudienceSubscribesAndUnsubscribesRequest setDailyAudienceSubscribesAndUnsubscribesRequest
    ) {
        long amount = setDailyAudienceSubscribesAndUnsubscribesRequest.getAmount();
        return audienceService.setDailyAudienceSubscribes(amount);
    }

    @Override
    public ResponseEntity<Void> setDailyAudienceUnsubscribes(
            SetDailyAudienceSubscribesAndUnsubscribesRequest setDailyAudienceSubscribesAndUnsubscribesRequest
    ) {
        long amount = setDailyAudienceSubscribesAndUnsubscribesRequest.getAmount();
        return audienceService.setDailyAudienceUnsubscribes(amount);
    }

}
