package ru.live4code.social_network.ai.internal.auth.exception.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.live4code.social_network.ai.generated.model.ClientAuthSignUpErrorResponse;
import ru.live4code.social_network.ai.internal.auth.exception.AuthRuntimeException;
import ru.live4code.social_network.ai.internal.auth.exception.error.*;

@ControllerAdvice
public class AuthControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            EmailExistsException.class,
            EmailNotValidException.class,
            SmallLengthException.class,
            NotCreatedException.class,
            PasswordsNotEqualsException.class
    })
    protected ResponseEntity<ClientAuthSignUpErrorResponse> handleExceptions(AuthRuntimeException exception) {
        var response = new ClientAuthSignUpErrorResponse();
        response.setError(exception.getErrorType());
        return ResponseEntity.badRequest().body(response);
    }

}
