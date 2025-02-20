package ru.live4code.social_network.ai.internal.publications.exception.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.live4code.social_network.ai.generated.model.EditPublicationForClientErrorResponse;
import ru.live4code.social_network.ai.internal.publications.exception.PublicationRuntimeException;
import ru.live4code.social_network.ai.internal.publications.exception.error.*;

@ControllerAdvice
public class PublicationControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            AlreadyPublishedException.class,
            PublicationDateNotValidException.class,
            PublicationTimeNotValidException.class,
            PublicationDateOutOfTariffBordersException.class,
            PastPublicationForbiddenException.class
    })
    protected ResponseEntity<EditPublicationForClientErrorResponse> handleExceptions(
            PublicationRuntimeException exception
    ) {
        var response = new EditPublicationForClientErrorResponse();
        response.setError(exception.getErrorType());
        return ResponseEntity.badRequest().body(response);
    }

}
