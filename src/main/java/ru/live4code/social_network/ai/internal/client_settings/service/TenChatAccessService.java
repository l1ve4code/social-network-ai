package ru.live4code.social_network.ai.internal.client_settings.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import ru.live4code.social_network.ai.external.exception.EmptyResponseBodyException;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatToken;
import ru.live4code.social_network.ai.external.tenchat.service.TenChatService;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.client_settings.dao.TenChatCredentialsDao;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone.ClientPhoneAlreadyAuthorizedException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone.PhoneNotValidException;
import ru.live4code.social_network.ai.internal.client_settings.model.RefreshCredentials;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenChatAccessService {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    private final ClientInfoService clientInfoService;
    private final TenChatService tenChatService;
    private final TenChatCredentialsDao tenChatCredentialsDao;

    public ResponseEntity<Void> sendAccessCodeToClientPhone(String phone) {

        Phonenumber.PhoneNumber number;

        try {
            number = phoneNumberUtil.parse(phone, "RU");
        } catch (NumberParseException e) {
            throw new PhoneNotValidException();
        }

        Client client = clientInfoService.getCurrentClient();
        String phoneForTenChat = String.format("%s%s", number.getCountryCode(), number.getNationalNumber());

        if (tenChatCredentialsDao.isTokensExistsByClientId(client.getId())) {
            throw new ClientPhoneAlreadyAuthorizedException();
        }

        tenChatService.sendVerificationCode(phoneForTenChat);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> sendAccessCodeToTenChatAndSaveCredentials(String phone, String code) {
        TenChatToken credentials = tenChatService.verifyCode(phone, code);
        Client client = clientInfoService.getCurrentClient();
        tenChatCredentialsDao.insertUserTokens(
                client.getId(),
                phone,
                credentials.accessToken(),
                credentials.refreshToken()
        );
        return ResponseEntity.ok().build();
    }

    public void actualizeClientTenChatCredentials() {
        List<RefreshCredentials> credentials = tenChatCredentialsDao.getClientCredentials();

        if (credentials.isEmpty()) {
            log.info("There no tokens for refresh!");
            return;
        }

        log.warn("{} will be refreshed!", credentials.size());

        Map<Long, TenChatToken> updatedTokens = new HashMap<>();

        for (var credential : credentials) {
            long clientId = credential.clientId();
            String token = credential.refreshToken();
            try {
                var tenChatToken = tenChatService.invokeNewTokens(token);
                updatedTokens.put(clientId, tenChatToken);
                log.info("Tokens were updated for client_id: {}", clientId);
            } catch (UnsupportedOperationException | IllegalArgumentException | EmptyResponseBodyException e) {
                log.error("Can't refresh token for client_id: {}! Message: {}", clientId, e.getMessage());
            }
        }

        tenChatCredentialsDao.updateCredentialsData(updatedTokens);
        log.info("{} tokens were successfully updated!", updatedTokens.size());
    }

}
