package ru.live4code.social_network.ai.external.chat_gpt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;
import ru.live4code.social_network.ai.external.image_gpt.model.KandinskyGPTGenerationResponse;
import ru.live4code.social_network.ai.external.exception.EmptyResponseBodyException;
import ru.live4code.social_network.ai.external.exception.WrongStatusException;
import ru.live4code.social_network.ai.utils.annotation.Service;

@Service
@RequiredArgsConstructor
public class RetryService {

    private final RestTemplate restTemplate;

    @Retryable(maxAttempts = 10, backoff = @Backoff(delay = 10000L),
            retryFor = {EmptyResponseBodyException.class, WrongStatusException.class})
    public String[] getStringArrayOrNull(String link, HttpEntity<Void> entity) {

        var response = restTemplate.exchange(link, HttpMethod.GET, entity, KandinskyGPTGenerationResponse.class);

        KandinskyGPTGenerationResponse body = response.getBody();

        if (body == null) {
            throw new EmptyResponseBodyException("No body data while getting image-gpt uuid!");
        }

        if (!body.getStatus().equals("DONE")) {
            throw new WrongStatusException("Waiting DONE status from GPT!");
        }

        return body.getImages();

    }

}
