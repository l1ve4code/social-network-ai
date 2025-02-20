package ru.live4code.social_network.ai.internal.direction.exception.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.live4code.social_network.ai.generated.model.PublicationsDirectionErrorResponse;
import ru.live4code.social_network.ai.internal.direction.exception.DirectionRuntimeException;
import ru.live4code.social_network.ai.internal.direction.exception.error.AlreadySavedException;
import ru.live4code.social_network.ai.internal.direction.exception.error.TooLowWordsException;

@ControllerAdvice
public class DirectionControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            AlreadySavedException.class,
            TooLowWordsException.class,
    })
    protected ResponseEntity<PublicationsDirectionErrorResponse> handleExceptions(DirectionRuntimeException exception) {
        var response = new PublicationsDirectionErrorResponse();
        response.setError(exception.getErrorType());
        return ResponseEntity.badRequest().body(response);
    }

}
