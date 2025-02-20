package ru.live4code.social_network.ai.internal.auth.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.live4code.social_network.ai.generated.model.ClientJwtResponse;
import ru.live4code.social_network.ai.internal.auth.dao.RecoverTokenDao;
import ru.live4code.social_network.ai.internal.auth.exception.error.*;
import ru.live4code.social_network.ai.internal.auth.model.RefreshToken;
import ru.live4code.social_network.ai.internal.client_info.dao.ClientDao;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.utils.annotation.Service;
import ru.live4code.social_network.ai.utils.environment.service.EnvironmentService;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String IS_AUTH_VALIDATION_ENABLED_ENV = "AuthenticationService.is-auth-validation.enabled";
    private static final Long MINUTE_LIFE_TIME_RECOVER_TOKEN = 10L;
    private static final String EMAIL_REGEXP = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    @Value("${application.host.address}")
    private String applicationLink;

    @Value("${spring.mail.username}")
    private String senderAddress;

    private final ClientDao clientDao;
    private final JwtService jwtService;
    private final ClientInfoService clientInfoService;
    private final RefreshTokenService refreshTokenService;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final RecoverTokenDao recoverTokenDao;
    private final AuthenticationManager authenticationManager;
    private final TransactionTemplate transactionTemplate;
    private final EnvironmentService environmentService;

    @Transactional
    public ResponseEntity<Void> signUp(String email, String password, String passwordConfirmation) {

        boolean isAuthValidationEnabled = isAuthValidationEnabled();

        if (isAuthValidationEnabled) {
            if (!Pattern.matches(EMAIL_REGEXP, email)) {
                throw new EmailNotValidException();
            }

            if (!password.equals(passwordConfirmation)) {
                throw new PasswordsNotEqualsException();
            }

            if (password.length() < 8) {
                throw new SmallLengthException();
            }
        }

        var encodedPassword = passwordEncoder.encode(password);
        try {
            clientDao.insertClient(email, encodedPassword, !isAuthValidationEnabled);
        } catch (DataAccessException accessException) {
            throw new EmailExistsException();
        }

        @Nullable Long clientId = clientDao.getClientIdByEmail(email);
        if (clientId == null) {
            throw new NotCreatedException();
        }

        if (isAuthValidationEnabled) {
            var token = UUID.randomUUID().toString();
            clientDao.insertConfirmationToken(token, clientId);

            sendValidationEmail(email, token);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    public ResponseEntity<ClientJwtResponse> signIn(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException exception) {
            return ResponseEntity.notFound().build();
        }

        Client client = clientInfoService.getClientByEmail(email);

        String accessToken = jwtService.generateToken(client);
        String refreshToken = refreshTokenService.createRefreshToken(client.getId());

        var jwtResponse = new ClientJwtResponse(accessToken, refreshToken);
        return ResponseEntity.ok(jwtResponse);
    }

    public ResponseEntity<ClientJwtResponse> refreshClientAccessToken(String token) {

        @Nullable RefreshToken refreshToken = refreshTokenService.getTokenIfNotExpiredAndExists(token);
        if (refreshToken == null) {
            return ResponseEntity.notFound().build();
        }

        long clientId = refreshToken.clientId();

        Client client = clientInfoService.getClientById(clientId);

        String accessToken = jwtService.generateToken(client);
        String newRefreshToken = refreshTokenService.createRefreshToken(clientId);

        refreshTokenService.deleteOldRefreshToken(token);

        var jwtResponse = new ClientJwtResponse(accessToken, newRefreshToken);
        return ResponseEntity.ok(jwtResponse);
    }

    public ResponseEntity<Void> validate(String token) {

        Long clientId = clientDao.getClientIdByConfirmationToken(token);

        if (clientId == null) {
            return ResponseEntity.notFound().build();
        }

        clientDao.enableClientById(clientId);
        clientDao.deleteConfirmationToken(token);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> sendPasswordRecoverLinkToClientEmail(String email) {

        @Nullable Client client = clientDao.getClientByEmail(email);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        long clientId = client.getId();
        if (recoverTokenDao.isFreshRecoverTokenExists(clientId)) {
            return ResponseEntity.badRequest().build();
        }

        StringBuilder builder = new StringBuilder(UUID.randomUUID().toString());
        builder.replace(6, builder.length() - 1, "");
        String recoverToken = builder.toString();

        OffsetDateTime recoverTokenLifeTime = OffsetDateTime.now().plusMinutes(MINUTE_LIFE_TIME_RECOVER_TOKEN);
        recoverTokenDao.insertRecoverToken(clientId, recoverToken, recoverTokenLifeTime);

        sendRecoverEmail(email, recoverToken);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> useClientRecoveredPassword(
            String recoverToken,
            String password,
            String passwordConfirmation
    ) {

        @Nullable Long clientId = recoverTokenDao.getClientIdByToken(recoverToken);
        if (clientId == null) {
            return ResponseEntity.notFound().build();
        }

        if (!password.equals(passwordConfirmation)) {
            return ResponseEntity.badRequest().build();
        }

        if (password.length() < 8) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        String encodedPassword = passwordEncoder.encode(password);
        transactionTemplate.executeWithoutResult(__ -> {
            clientDao.updateClientPasswordById(clientId, encodedPassword);
            recoverTokenDao.deleteToken(recoverToken);
        });

        return ResponseEntity.ok().build();
    }

    private void sendRecoverEmail(String email, String token) {
        var mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setFrom(senderAddress);
        mailMessage.setSubject("[PromoteLab] Восстановление пароля");
        mailMessage.setText(String.format("Код для восстановления пароля -> %s", token));
        javaMailSender.send(mailMessage);
    }

    private void sendValidationEmail(String email, String token) {
        var mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setFrom(senderAddress);
        mailMessage.setSubject("[PromoteLab] Подтверждение почты");
        mailMessage.setText(String.format("%s/api/v1/auth/validate?token=%s", applicationLink, token));
        javaMailSender.send(mailMessage);
    }

    private boolean isAuthValidationEnabled() {
        return environmentService.getBooleanValueOrDefault(IS_AUTH_VALIDATION_ENABLED_ENV, true);
    }

}
