package ru.live4code.social_network.ai.internal.client_settings.exception.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendCodeErrorResponse;
import ru.live4code.social_network.ai.generated.model.ClientSettingsTenchatSendPhoneErrorResponse;
import ru.live4code.social_network.ai.internal.client_settings.exception.ClientSettingsRuntimeException;
import ru.live4code.social_network.ai.generated.model.ClientSettingsEditPasswordErrorResponse;
import ru.live4code.social_network.ai.internal.client_settings.exception.TenchatCodeRuntimeException;
import ru.live4code.social_network.ai.internal.client_settings.exception.TenchatPhoneRuntimeException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.NewPasswordAlreadyUsesException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.NewPasswordsNotEqualsException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.NewPasswordIsWeakException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.client.CurrentPasswordNotValidException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.CodeLifeTimeEndException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.CodeNotValidException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.TooManyCodeAttemptsException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_code.UnexpectedCodeResponseException;
import ru.live4code.social_network.ai.internal.client_settings.exception.error.tenchat_phone.*;

@ControllerAdvice
public class ClientSettingsControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            NewPasswordAlreadyUsesException.class,
            NewPasswordsNotEqualsException.class,
            NewPasswordIsWeakException.class,
            CurrentPasswordNotValidException.class
    })
    protected ResponseEntity<ClientSettingsEditPasswordErrorResponse> handleClientExceptions(
            ClientSettingsRuntimeException exception
    ) {
        var response = new ClientSettingsEditPasswordErrorResponse();
        response.setError(exception.getErrorType());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            PhoneNotValidException.class,
            PhoneWaitException.class,
            TooManyPhoneAttemptsException.class,
            UnexpectedPhoneResponseException.class,
            ClientPhoneAlreadyAuthorizedException.class
    })
    protected ResponseEntity<ClientSettingsTenchatSendPhoneErrorResponse> handleTenChatPhoneExceptions(
            TenchatPhoneRuntimeException exception
    ) {
        var response = new ClientSettingsTenchatSendPhoneErrorResponse();
        response.setError(exception.getErrorType());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            CodeLifeTimeEndException.class,
            CodeNotValidException.class,
            TooManyCodeAttemptsException.class,
            UnexpectedCodeResponseException.class
    })
    protected ResponseEntity<ClientSettingsTenchatSendCodeErrorResponse> handleTenChatCodeExceptions(
            TenchatCodeRuntimeException exception
    ) {
        var response = new ClientSettingsTenchatSendCodeErrorResponse();
        response.setError(exception.getErrorType());
        return ResponseEntity.badRequest().body(response);
    }

}
