package ru.live4code.social_network.ai.internal.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.live4code.social_network.ai.generated.api.AuthApi;
import ru.live4code.social_network.ai.generated.model.*;
import ru.live4code.social_network.ai.internal.auth.service.AuthenticationService;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthenticationService authenticationService;

    @Override
    public ResponseEntity<ClientJwtResponse> signInClient(ClientAuthSignInRequest clientAuthSignInRequest) {
        String email = clientAuthSignInRequest.getEmail();
        String password = clientAuthSignInRequest.getPassword();
        return authenticationService.signIn(email, password);
    }

    @Override
    public ResponseEntity<Void> signUpClient(ClientAuthSignUpRequest clientAuthSignUpRequest) {
        String email = clientAuthSignUpRequest.getEmail();
        String password = clientAuthSignUpRequest.getPassword();
        String passwordConfirmation = clientAuthSignUpRequest.getPasswordConfirmation();
        return authenticationService.signUp(email, password, passwordConfirmation);
    }

    @Override
    public ResponseEntity<ClientJwtResponse> refreshClientAccessToken(
            RefreshClientAccessTokenRequest refreshClientAccessTokenRequest
    ) {
        String token = refreshClientAccessTokenRequest.getRefreshToken();
        return authenticationService.refreshClientAccessToken(token);
    }

    @Override
    public ResponseEntity<Void> validateEmailAddress(String token) {
        return authenticationService.validate(token);
    }

    @Override
    public ResponseEntity<Void> sendPasswordRecoverLinkToClientEmail(
            SendPasswordRecoverLinkToClientEmailRequest sendPasswordRecoverLinkToClientEmailRequest
    ) {
        String email = sendPasswordRecoverLinkToClientEmailRequest.getEmail();
        return authenticationService.sendPasswordRecoverLinkToClientEmail(email);
    }

    @Override
    public ResponseEntity<Void> useClientRecoveredPassword(
            UseClientRecoveredPasswordRequest useClientRecoveredPasswordRequest
    ) {
        String recoverToken = useClientRecoveredPasswordRequest.getRecoverToken();
        String password = useClientRecoveredPasswordRequest.getPassword();
        String passwordConfirmation = useClientRecoveredPasswordRequest.getPasswordConfirmation();
        return authenticationService.useClientRecoveredPassword(recoverToken, password, passwordConfirmation);
    }

}
