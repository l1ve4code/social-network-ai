package ru.live4code.social_network.ai.external.yoomoney.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.live4code.social_network.ai.external.exception.EmptyResponseBodyException;
import ru.live4code.social_network.ai.external.exception.PaymentException;
import ru.live4code.social_network.ai.external.yoomoney.model.PaymentDetails;
import ru.live4code.social_network.ai.external.yoomoney.model.PaymentStatus;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoomoneyService {

    private final static String PAYMENTS_URL = "v3/payments";

    @Value("${application.host.address}")
    private String applicationLink;

    @Value("${external.api.yookassa.url}")
    private String yoomoneyLink;

    @Value("${external.api.yookassa.id}")
    private String yoomoneyId;

    @Value("${external.api.yookassa.secret}")
    private String yoomoneySecret;

    private final RestTemplate restTemplate;

    @NotNull
    public PaymentDetails createPaymentAndGetDetails(String tariffName, long amount) {

        HttpHeaders headers = getAuthorizationHeaders();
        headers.set("Idempotence-Key", UUID.randomUUID().toString());

        JSONObject jsonAmount = new JSONObject(Map.of(
                "value", amount,
                "currency", "RUB"
        ));
        JSONObject jsonConfirmation = new JSONObject(Map.of(
                "type", "redirect",
                "return_url", applicationLink
        ));
        JSONObject jsonObject = new JSONObject(Map.of(
                "amount", jsonAmount,
                "capture", true,
                "confirmation", jsonConfirmation,
                "description", String.format("Оплата тарифа: %s, на месяц.", tariffName)
        ));

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);

        String link = String.format("%s/%s", yoomoneyLink, PAYMENTS_URL);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            throw new PaymentException(exception.getMessage());
        }

        String body = response.getBody();

        if (body == null) {
            throw new EmptyResponseBodyException("Got empty body while creating payment!");
        }

        JSONObject result = new JSONObject(body);
        String id = result.getString("id");

        JSONObject confirmation = result.getJSONObject("confirmation");
        String confirmationUrl = confirmation.getString("confirmation_url");

        return new PaymentDetails(id, confirmationUrl);
    }

    public PaymentStatus getPaymentStatus(String paymentId) {

        HttpHeaders headers = getAuthorizationHeaders();

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String link = String.format("%s/%s/%s", yoomoneyLink, PAYMENTS_URL, paymentId);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.GET, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            throw new PaymentException(exception.getMessage());
        }

        String body = response.getBody();

        if (body == null) {
            throw new EmptyResponseBodyException("Got empty body while getting payment status!");
        }

        JSONObject result = new JSONObject(body);
        String status = result.getString("status");

        return PaymentStatus.findStatus(status);
    }

    private HttpHeaders getAuthorizationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(yoomoneyId, yoomoneySecret);
        return headers;
    }

}
