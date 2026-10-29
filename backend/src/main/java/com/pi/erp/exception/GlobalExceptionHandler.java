package com.pi.erp.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseErrorDTO> handleAccessDenied(
        AccessDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseErrorDTO(
            403, "Forbidden", "Permission denied.", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ResponseErrorDTO> handleStatus(
        ResponseStatusException exception
    ) {
        HttpStatusCode status = exception.getStatusCode();
        HttpStatus value = HttpStatus.resolve(status.value());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (status.value() == 429) {
            response.header("Retry-After", "60");
        }
        return response.body(new ResponseErrorDTO(
            status.value(), value == null ? "HTTP Error" : value.getReasonPhrase(),
            value == null ? "Request could not be processed." : value.getReasonPhrase() + ".",
            LocalDateTime.now()
        ));
    }

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseErrorDTO> handleResourceNotFound(
        ResourceNotFoundException exception
    ) {
        ResponseErrorDTO error = new ResponseErrorDTO(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            exception.getMessage(),
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseErrorDTO> handleIllegalArgument(
        IllegalArgumentException exception
    ) {
        ResponseErrorDTO error = new ResponseErrorDTO(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            exception.getMessage(),
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseErrorDTO> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        String message = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));

        ResponseErrorDTO error = new ResponseErrorDTO(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseErrorDTO> handleUnreadableMessage(
        HttpMessageNotReadableException exception
    ) {
        ResponseErrorDTO error = new ResponseErrorDTO(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Request body is missing or contains invalid data.",
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseErrorDTO> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception
    ) {
        ResponseErrorDTO error = new ResponseErrorDTO(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Invalid value for parameter '" + exception.getName() + "'.",
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ResponseErrorDTO> handlePessimisticLocking(
        PessimisticLockingFailureException exception
    ) {
        ResponseErrorDTO error = new ResponseErrorDTO(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "Operation conflicts with another transaction. Please try again.",
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseErrorDTO> handleDataIntegrity(
        DataIntegrityViolationException exception
    ) {
        ResponseErrorDTO error = new ResponseErrorDTO(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "Operation conflicts with existing records or database constraints.",
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseErrorDTO> handleUnexpected(Exception exception) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        HttpHeaders headers = HttpHeaders.EMPTY;

        if (exception instanceof ErrorResponse errorResponse) {
            status = errorResponse.getStatusCode();
            headers = errorResponse.getHeaders();
        } else {
            ResponseStatus responseStatus = AnnotationUtils.findAnnotation(
                exception.getClass(),
                ResponseStatus.class
            );

            if (responseStatus != null) {
                status = responseStatus.code();
            }
        }

        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        String errorType = httpStatus != null ? httpStatus.getReasonPhrase() : "HTTP Error";
        String message;

        if (status.is5xxServerError()) {
            logger.error("Unexpected error while processing request.", exception);
            message = "An unexpected error occurred.";
        } else if (httpStatus != null) {
            message = httpStatus.getReasonPhrase() + ".";
        } else {
            message = "Request could not be processed.";
        }

        ResponseErrorDTO error = new ResponseErrorDTO(
            status.value(),
            errorType,
            message,
            LocalDateTime.now()
        );

        return ResponseEntity.status(status).headers(headers).body(error);
    }
}
