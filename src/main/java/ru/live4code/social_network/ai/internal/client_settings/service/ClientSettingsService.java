package ru.live4code.social_network.ai.internal.client_settings.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.live4code.social_network.ai.internal.client_info.dao.ClientDao;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.NewPasswordAlreadyUsesException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.NewPasswordsNotEqualsException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.NewPasswordIsWeakException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.CurrentPasswordNotValidException;
import ru.live4code.social_network.ai.utils.annotation.Service;

@Service
@RequiredArgsConstructor
public class ClientSettingsService {

    private final ClientDao clientDao;
    private final ClientInfoService clientInfoService;
    private final PasswordEncoder passwordEncoder;

    public ResponseEntity<Void> changeClientNameSurname(String name, String surname) {
        var email = clientInfoService.getCurrentClientEmail();
        clientDao.updateClientNameSurnameByEmail(email, name, surname);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> changeClientPassword(
            String passedPassword,
            String newPassword,
            String confirmationPassword
    ) {

        var client = clientInfoService.getCurrentClient();
        var encodedCurrentPassword = client.getPassword();

        if (!passwordEncoder.matches(passedPassword, encodedCurrentPassword)) {
            throw new CurrentPasswordNotValidException();
        }

        if (!newPassword.equals(confirmationPassword)) {
            throw new NewPasswordsNotEqualsException();
        }

        if (passwordEncoder.matches(newPassword, encodedCurrentPassword)) {
            throw new NewPasswordAlreadyUsesException();
        }

        if (newPassword.length() < 8) {
            throw new NewPasswordIsWeakException();
        }

        var email = client.getEmail();
        var encodedNewPassword = passwordEncoder.encode(newPassword);
        clientDao.updateClientPasswordByEmail(email, encodedNewPassword);

        return ResponseEntity.ok().build();
    }

}
