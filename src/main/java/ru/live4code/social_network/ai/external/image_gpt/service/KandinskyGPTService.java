package ru.live4code.social_network.ai.external.image_gpt.service;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.live4code.social_network.ai.external.exception.EmptyResponseBodyException;
import ru.live4code.social_network.ai.external.exception.WrongSizeException;
import ru.live4code.social_network.ai.external.image_gpt.model.KandinskyGPTModelResponse;
import ru.live4code.social_network.ai.external.image_gpt.model.KandinskyGPTUUIDResponse;
import ru.live4code.social_network.ai.external.chat_gpt.service.RetryService;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KandinskyGPTService {

    private final static String MODEL_URL = "key/api/v1/models";
    private final static String GENERATE_URL = "key/api/v1/text2image/run";
    private final static String STATUS_URL = "key/api/v1/text2image/status";

    @Value("${kandinsky.x-key}")
    private String xKey;

    @Value("${kandinsky.x-secret}")
    private String xSecret;

    private final String imageGPTLink;
    private final RestTemplate restTemplate;
    private final RetryService retryService;

    public String getImageByPrompt(String prompt) {

        String uuidOfGeneration = getUUIDofGeneration(prompt);

        HttpHeaders headers = getAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String link = String.format("%s/%s/%s", imageGPTLink, STATUS_URL, uuidOfGeneration);

        String[] images = retryService.getStringArrayOrNull(link, entity);

        if (images.length < 1) {
            throw new WrongSizeException("No images after waiting!");
        }

        return images[0];
    }

    private String getUUIDofGeneration(String prompt) {

        String modelId = getFirstGPTModel();

        HttpHeaders totalHeaders = getAuthHeaders();
        totalHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        JSONObject jsonObject = new JSONObject(Map.of(
                "type", "GENERATE",
                "numImages", "1",
                "width", "1024",
                "height", "1024",
                "generateParams", new JSONObject(Map.of("query", prompt))
        ));

        var paramsHeaders = new HttpHeaders();
        paramsHeaders.setContentType(MediaType.APPLICATION_JSON);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("model_id", modelId);
        form.add("params", new HttpEntity<>(jsonObject.toString(), paramsHeaders));

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, totalHeaders);

        String link = String.format("%s/%s", imageGPTLink, GENERATE_URL);

        var response = restTemplate.exchange(link, HttpMethod.POST, entity, KandinskyGPTUUIDResponse.class);

        KandinskyGPTUUIDResponse body = response.getBody();

        if (body == null) {
            throw new EmptyResponseBodyException("No body data while getting image-gpt uuid!");
        }

        return body.getUuid();
    }

    private String getFirstGPTModel() {

        HttpHeaders headers = getAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String link = String.format("%s/%s", imageGPTLink, MODEL_URL);

        var response = restTemplate.exchange(link, HttpMethod.GET, entity, KandinskyGPTModelResponse[].class);

        var body = response.getBody();

        if (body == null || body.length < 1) {
            throw new EmptyResponseBodyException("No body data while getting image-gpt models!");
        }

        return body[0].getId();
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Key", "Key %s".formatted(xKey));
        headers.set("X-Secret", "Secret %s".formatted(xSecret));
        return headers;
    }

}
