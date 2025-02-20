package ru.live4code.social_network.ai.external.tenchat.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.live4code.social_network.ai.external.exception.ActionLimitException;
import ru.live4code.social_network.ai.external.exception.EmptyResponseBodyException;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatAccountInfo;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatConnectionType;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatToken;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.CodeLifeTimeEndException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.CodeNotValidException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.TooManyCodeAttemptsException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.UnexpectedCodeResponseException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone.PhoneWaitException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone.TooManyPhoneAttemptsException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone.UnexpectedPhoneResponseException;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.encryption.EncryptionService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenChatService {

    private final static String POST_URL = "gostinder/api/web/auth/user/post";
    private final static String POST_UPLOAD_FILE_URL = "gostinder/api/web/auth/user/post/upload-file?fileType=PICTURE";
    private final static String SEND_VERIFICATION_CODE_URL = "gostinder/api/web/send-auth-code";
    private final static String VERIFY_CODE_URL = "gostinder/api/web/verify-auth-code";
    private final static String GET_SESSION_ID_URL = "gostinder/api/web/auth/account";
    private final static String INVOKE_NEW_TOKENS_URL = "vbc-oauth2-gostinder/oauth/token";
    private final static String GET_PEOPLE_LIST_URL = "gostinder/api/web/auth/connection/filter";
    private final static String PEOPLE_SUBSCRIBE_URL = "gostinder/api/web/auth/account/username/%s/subscribe";
    private final static String PEOPLE_UNSUBSCRIBE_URL = "gostinder/api/web/auth/account/username/%s/unsubscribe";
    private final static String SUBSCRIPTIONS_LIST_URL = "gostinder/api/web/auth/account/search?page=0&size=%s";

    private final String tenChatLink;
    private final RestTemplate restTemplate;
    private final EncryptionService encryptionService;

    public TenChatToken verifyCode(String phoneNumber, String code) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)");

        JSONObject object = new JSONObject(Map.of(
                "phone", phoneNumber,
                "code", code
        ));

        HttpEntity<String> entity = new HttpEntity<>(object.toString(), headers);

        String link = String.format("%s/%s", tenChatLink, VERIFY_CODE_URL);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            var jsonErrorMessage = new JSONObject(exception.getResponseBodyAsString());
            var exceptionType = jsonErrorMessage.getString("abbr");
            switch (exceptionType) {
                case "INVALID_AMOUNT" -> throw new TooManyCodeAttemptsException();
                case "INVALID_VALUE" -> throw new CodeNotValidException();
                case "INVALID_DATE" -> throw new CodeLifeTimeEndException();
                default -> throw new UnexpectedCodeResponseException();
            }
        }

        var body = response.getBody();

        if (body == null) {
            throw new UnexpectedCodeResponseException();
        }

        var result = new JSONObject(body);
        var accessTokenJSON = result.getJSONObject("accessToken");
        var refreshTokenJSON = result.getJSONObject("refreshToken");

        var encryptedAccessToken = encryptionService.encrypt(accessTokenJSON.getString("tokenValue"));
        var encryptedRefreshToken = encryptionService.encrypt(refreshTokenJSON.getString("tokenValue"));

        return new TenChatToken(encryptedAccessToken, encryptedRefreshToken);
    }

    public boolean sendVerificationCode(String phoneNumber) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)");

        JSONObject object = new JSONObject(Map.of(
                "phone", phoneNumber,
                "countryOksm", "643"
        ));

        HttpEntity<String> entity = new HttpEntity<>(object.toString(), headers);

        String link = String.format("%s/%s", tenChatLink, SEND_VERIFICATION_CODE_URL);

        ResponseEntity<Void> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, Void.class);
        } catch (HttpStatusCodeException exception) {
            var jsonErrorMessage = new JSONObject(exception.getResponseBodyAsString());
            var exceptionType = jsonErrorMessage.getString("abbr");
            switch (exceptionType) {
                case "INVALID_AMOUNT" -> throw new TooManyPhoneAttemptsException();
                case "INVALID_DATE" -> throw new PhoneWaitException();
                default -> throw new UnexpectedPhoneResponseException();
            }
        }

        var statusCode = response.getStatusCode();

        return statusCode.is2xxSuccessful();
    }

    public TenChatToken invokeNewTokens(String encryptedRefreshToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        var decryptedRefreshToken = encryptionService.decrypt(encryptedRefreshToken);

        String link = String.format(
                "%s/%s?refresh_token=%s&grant_type=refresh_token",
                tenChatLink,
                INVOKE_NEW_TOKENS_URL,
                decryptedRefreshToken
        );

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            var jsonErrorMessage = new JSONObject(exception.getResponseBodyAsString());
            var exceptionType = jsonErrorMessage.getString("error");
            if (exceptionType.equals("invalid_grant")) {
                throw new IllegalArgumentException("Client token is not valid!");
            }
            throw new UnsupportedOperationException(
                    String.format("Unexpected error from TenChat: %s", exception.getMessage())
            );
        }

        var body = response.getBody();

        if (body == null) {
            throw new EmptyResponseBodyException("No body data while getting tokens!");
        }

        var result = new JSONObject(body);
        var newAccessToken = result.getString("access_token");
        var newRefreshToken = result.getString("refresh_token");

        var encryptedNewAccessToken = encryptionService.encrypt(newAccessToken);
        var encryptedNewRefreshToken = encryptionService.encrypt(newRefreshToken);

        return new TenChatToken(encryptedNewAccessToken, encryptedNewRefreshToken);
    }

    @Nullable
    public TenChatAccountInfo getAccountInfo(String decryptedAccessToken) {

        HttpHeaders httpHeaders = buildAuthorizationHeadersByAccessToken(decryptedAccessToken);

        HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);

        String link = String.format("%s/%s", tenChatLink, GET_SESSION_ID_URL);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.GET, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            return null;
        }

        var headers = response.getHeaders();
        var body = response.getBody();

        if (body == null) {
            log.error("No body data while getting account info!");
            return null;
        }

        if (!headers.containsKey("Set-Cookie")) {
            log.error("No header data while getting account info!");
            return null;
        }

        var setCookieHeaderData = headers.get("Set-Cookie").stream().findFirst().orElseThrow();
        var sessionId = setCookieHeaderData.split("SESSION=")[1].split(";")[0];
        if (sessionId == null || sessionId.isEmpty()) {
            log.error("Session ID is empty!");
            return null;
        }

        var jsonResponse = new JSONObject(body);

        return new TenChatAccountInfo(
                sessionId,
                jsonResponse.getString("username"),
                jsonResponse.getLong("subscriberCount"),
                jsonResponse.getLong("subscriptionCount")
        );
    }

    public boolean doPeopleSubscribeAction(String sessionId, String username, boolean needSubscribe) {

        HttpHeaders headers = buildAuthorizationHeadersBySession(sessionId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        var urlWithUsername = needSubscribe ?
                String.format(PEOPLE_SUBSCRIBE_URL, username) :
                String.format(PEOPLE_UNSUBSCRIBE_URL, username);

        String link = String.format("%s/%s", tenChatLink, urlWithUsername);

        ResponseEntity<Void> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, Void.class);
        } catch (HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            var jsonErrorMessage = new JSONObject(exception.getResponseBodyAsString());
            var exceptionType = jsonErrorMessage.getString("abbr");
            if (exceptionType.equals("INVALID_VALUE")) {
                throw new ActionLimitException("Subscribe action limit!");
            }
            return false;
        }

        var statusCode = response.getStatusCode();

        return statusCode.is2xxSuccessful();
    }

    public List<String> getFilteredSubscriptionUserNames(
            String sessionId,
            String userName,
            long peopleAmount,
            boolean isSubscribers
    ) {

        HttpHeaders headers = buildAuthorizationHeadersBySession(sessionId);;

        JSONObject object = new JSONObject(Map.of(
                "inUserSubscribers", isSubscribers,
                "inUserSubscriptions", !isSubscribers,
                "username", userName
        ));

        HttpEntity<String> entity = new HttpEntity<>(object.toString(), headers);

        var linkWithSize = String.format(SUBSCRIPTIONS_LIST_URL, peopleAmount);

        String link = String.format("%s/%s", tenChatLink, linkWithSize);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            return List.of();
        }

        var body = response.getBody();

        if (body == null) {
            log.error("No body data while getting subscriptions!");
            return List.of();
        }

        var result = new ArrayList<String>();
        new JSONArray(body).forEach(item -> {
            var peopleInfoObject = (JSONObject) item;
            result.add(peopleInfoObject.getString("username"));
        });

        return result;
    }

    public List<String> getPeopleToSubscribe(
            String sessionId,
            TenChatConnectionType connectionType,
            long peopleAmount
    ) {

        HttpHeaders headers = buildAuthorizationHeadersBySession(sessionId);

        JSONObject object = new JSONObject(Map.of(
                "type", connectionType.getValue()
        ));

        HttpEntity<String> entity = new HttpEntity<>(object.toString(), headers);

        String link = String.format("%s/%s?page=0&size=%s", tenChatLink, GET_PEOPLE_LIST_URL, peopleAmount);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            return List.of();
        }

        var body = response.getBody();

        if (body == null) {
            log.error("No body data while getting people!");
            return List.of();
        }

        var result = new ArrayList<String>();
        new JSONArray(body).forEach(item -> {
            var peopleInfoObject = (JSONObject) item;
            result.add(peopleInfoObject.getString("username"));
        });

        return result;
    }

    public boolean publishPost(String sessionId, String title, String description, byte[] imageBytes) {

        HttpHeaders headers = buildAuthorizationHeadersBySession(sessionId);

        @Nullable Long imageUUID = sendImageAndGetUUID(sessionId, imageBytes);

        if (imageUUID == null) {
            return false;
        }

        JSONObject object = new JSONObject(Map.of(
                "adminTagTechNames", new JSONArray(),
                "attachments", new JSONArray(),
                "metadata", "",
                "pictures", new JSONArray(List.of(imageUUID)),
                "picturesDto", new JSONArray(List.of(new JSONObject(Map.of("id", imageUUID, "priority", 1)))),
                "tagCodes", new JSONArray(),
                "text", description,
                "title", title,
                "videos", new JSONArray(),
                "videosDto", new JSONArray()
        ));

        HttpEntity<String> entity = new HttpEntity<>(object.toString(), headers);

        String link = String.format("%s/%s", tenChatLink, POST_URL);

        ResponseEntity<Void> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, Void.class);
        } catch (HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            return false;
        }

        var statusCode = response.getStatusCode();

        return statusCode.is2xxSuccessful();
    }

    @Nullable
    private Long sendImageAndGetUUID(String sessionId, byte[] imageBytes) {

        HttpHeaders totalHeaders = buildAuthorizationHeadersBySession(sessionId);
        totalHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ContentDisposition disposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename(String.format("%s.jpg", UUID.randomUUID()))
                .build();
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        fileMap.set(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(imageBytes, fileMap);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.set("file", fileEntity);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, totalHeaders);

        String link = String.format("%s/%s", tenChatLink, POST_UPLOAD_FILE_URL);

        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(link, HttpMethod.POST, entity, String.class);
        } catch (HttpStatusCodeException exception) {
            log.error(exception.getMessage());
            return null;
        }

        var body = response.getBody();

        if (body == null) {
            log.error("Got nullable body, but expected TenChat image_id!");
            return null;
        }

        var result = new JSONObject(body);

        return result.getLong("fileId");
    }

    private HttpHeaders buildAuthorizationHeadersByAccessToken(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)");
        return headers;
    }

    private HttpHeaders buildAuthorizationHeadersBySession(String clientSession) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", String.format("SESSION=%s", clientSession));
        headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)");
        return headers;
    }

}
