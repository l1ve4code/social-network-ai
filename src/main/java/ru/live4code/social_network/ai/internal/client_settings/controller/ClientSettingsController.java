package ru.live4code.social_network.ai.internal.client_settings.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.SettingsApi;
import ru.live4code.social_network.ai.generated.model.ClientSettingsEditNameRequest;
import ru.live4code.social_network.ai.generated.model.ClientSettingsEditPasswordRequest;
import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendCodeRequest;
import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendPhoneRequest;
import ru.live4code.social_network.ai.internal.client_settings.service.ClientSettingsService;
import ru.live4code.social_network.ai.internal.client_settings.service.TenChatAccessService;

@RestController
@RequiredArgsConstructor
public class ClientSettingsController implements SettingsApi {

    private final TenChatAccessService tenChatAccessService;
    private final ClientSettingsService clientSettingsService;

    @Override
    public ResponseEntity<Void> changeClientName(ClientSettingsEditNameRequest clientSettingsEditNameRequest) {
        String name = clientSettingsEditNameRequest.getName();
        String surname = clientSettingsEditNameRequest.getSurname();
        return clientSettingsService.changeClientNameSurname(name, surname);
    }

    @Override
    public ResponseEntity<Void> changeClientPassword(
            ClientSettingsEditPasswordRequest clientSettingsEditPasswordRequest
    ) {
        String passedPassword = clientSettingsEditPasswordRequest.getCurrentPassword();
        String newPassword = clientSettingsEditPasswordRequest.getNewPassword();
        String confirmationPassword = clientSettingsEditPasswordRequest.getNewPasswordConfirmation();
        return clientSettingsService.changeClientPassword(passedPassword, newPassword, confirmationPassword);
    }

    @Override
    public ResponseEntity<Void> sendTenChatPhoneNumber(
            ClientSettingsTenchatSendPhoneRequest clientSettingsTenchatSendPhoneRequest
    ) {
        String phone = clientSettingsTenchatSendPhoneRequest.getPhone();
        return tenChatAccessService.sendAccessCodeToClientPhone(phone);
    }

    @Override
    public ResponseEntity<Void> sendTenChatValidationCode(
            ClientSettingsTenchatSendCodeRequest clientSettingsTenchatSendCodeRequest
    ) {
        String phone = clientSettingsTenchatSendCodeRequest.getPhone();
        String code = clientSettingsTenchatSendCodeRequest.getCode();
        return tenChatAccessService.sendAccessCodeToTenChatAndSaveCredentials(phone, code);
    }

}
