package com.example.thincontroller.order.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;

/**
 * Централизует HTTP error mapping, чтобы controllers занимались request/result
 * mapping и не повторяли обработку framework exceptions.
 */
@RestControllerAdvice
public class ApiErrors {

    /**
     * Преобразует ошибки Bean Validation в стабильное тело ответа 400.
     * Так validation формы запроса остается на HTTP boundary, а клиенты не
     * зависят от дефолтной сериализации ошибок Spring Boot.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
        var fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldViolation(error.getField(), messageOf(error.getDefaultMessage())))
                .sorted(Comparator.comparing(ApiErrorResponse.FieldViolation::field))
                .toList();

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed.", fields));
    }

    /**
     * Нормализует framework validation messages до того, как они станут частью
     * публичного API error body.
     */
    private String messageOf(String defaultMessage) {
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return "Invalid value.";
        }
        return defaultMessage;
    }
}
