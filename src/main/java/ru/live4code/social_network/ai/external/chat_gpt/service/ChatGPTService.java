package ru.live4code.social_network.ai.external.chat_gpt.service;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.live4code.social_network.ai.external.chat_gpt.model.ChatGPTChoicesResponse;
import ru.live4code.social_network.ai.external.exception.EmptyResponseBodyException;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatGPTService {

    private final String chatGPTLink;
    private final RestTemplate restTemplate;

    public String getChatGPTAnswer(String prompt) {

        HttpEntity<String> entity = getStringHttpEntity(prompt);

        String link = String.format("%s/%s", chatGPTLink, "v1/chat/completions");

        var response = restTemplate.exchange(link, HttpMethod.POST, entity, ChatGPTChoicesResponse.class);

        var body = response.getBody();

        if (body == null || body.getChoices().length < 1) {
            throw new EmptyResponseBodyException("No body data while getting chat-gpt text!");
        }

        return body.getChoices()[0].getMessage().getContent();
    }

    private HttpEntity<String> getStringHttpEntity(String prompt) {
        HttpHeaders headers = getHeaders();
        JSONObject json = new JSONObject(Map.of(
                "model", "gpt-3.5-turbo",
                "messages", new JSONArray(List.of(
                        new JSONObject(Map.of(
                                "role", "user",
                                "content", prompt
                        ))
                ))
        ));
        return new HttpEntity<>(json.toString(), headers);
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "*/*");
        headers.set("content-type", "application/json");
        headers.set("host", "beta.servergpts.com:2053");
        headers.set("origin", "https://trychatgpt.ru");
        headers.set("referer", "https://trychatgpt.ru/");
        headers.set("authorization", "Bearer sk-FfuckYouzeHKVk20PARtAT3BlbkFJyVBpgEPIP6Ui1dWExuvJ");
        headers.set("user-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.216 Safari/537.36");
        return headers;
    }

}
