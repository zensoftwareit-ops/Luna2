package it.zensoftware.luna2.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global Exception Handler for REST API
 * Gestisce validazione input, errori di business logic, errori di sistema
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Gestisce errori di validazione @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        logger.warn("Errore di validazione input: {}", ex.getMessage());

        List<FieldErrorDetail> fieldErrors = new ArrayList<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new FieldErrorDetail(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue() != null ? error.getRejectedValue().toString() : null
            ));
        }

        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validazione input non riuscita",
            LocalDateTime.now(),
            fieldErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Gestisce eccezioni SDI personalizzate
     */
    @ExceptionHandler(name = "SdiException")
    public ResponseEntity<ErrorResponse> handleSdiException(Exception ex, WebRequest request) {
        logger.error("Errore SDI: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Errore nella comunicazione con l'SDI: " + ex.getMessage(),
            LocalDateTime.now(),
            ex.getClass().getSimpleName()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Gestisce eccezioni generiche
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Errore non gestito: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Errore interno del server",
            LocalDateTime.now(),
            ex.getClass().getSimpleName()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Gestisce IllegalArgumentException (es. enum non valido)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        logger.warn("Argomento non valido: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Argomento non valido: " + ex.getMessage(),
            LocalDateTime.now(),
            "IllegalArgumentException"
        );

        return ResponseEntity.badRequest().body(response);
    }

    // ==================== RESPONSE CLASSES ====================

    public static class ErrorResponse {
        public int status;
        public String message;
        public LocalDateTime timestamp;
        public String exceptionType;

        public ErrorResponse(int status, String message, LocalDateTime timestamp, String exceptionType) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.exceptionType = exceptionType;
        }

        public ErrorResponse() {}

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getExceptionType() { return exceptionType; }
        public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        public List<FieldErrorDetail> fieldErrors;

        public ValidationErrorResponse(int status, String message,
                                      LocalDateTime timestamp,
                                      List<FieldErrorDetail> fieldErrors) {
            super(status, message, timestamp, "ValidationException");
            this.fieldErrors = fieldErrors;
        }

        public List<FieldErrorDetail> getFieldErrors() { return fieldErrors; }
        public void setFieldErrors(List<FieldErrorDetail> fieldErrors) { this.fieldErrors = fieldErrors; }
    }

    public static class FieldErrorDetail {
        public String field;
        public String message;
        public String rejectedValue;

        public FieldErrorDetail(String field, String message, String rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
        public String getRejectedValue() { return rejectedValue; }
    }
}
