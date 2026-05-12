package com.example.thincontroller.order.api;

import java.util.List;
import java.util.Objects;

/**
 * Стабильный HTTP DTO для ошибок на API boundary.
 * В Thin Controller форма ошибки принадлежит HTTP-слою, а бизнес-решения
 * остаются в use case и domain слоях.
 */
public record ApiErrorResponse(
        String code,
        String message,
        List<FieldViolation> fields
) {

    /**
     * Защитно проверяет и копирует данные ошибки, чтобы HTTP error contract
     * оставался стабильным после создания объекта.
     */
    public ApiErrorResponse {
        Objects.requireNonNull(code, "code must not be null.");
        Objects.requireNonNull(message, "message must not be null.");
        fields = List.copyOf(fields);
    }

    /**
     * Создает ошибку без field-level деталей для бизнес-исходов, которые
     * controller маппит из типизированных результатов use case.
     */
    public static ApiErrorResponse withoutFields(String code, String message) {
        return new ApiErrorResponse(code, message, List.of());
    }

    /**
     * Описывает одно невалидное поле запроса без утечки Spring exception
     * классов в публичный API contract.
     */
    public record FieldViolation(
            String field,
            String message
    ) {

        /**
         * Проверяет полноту field error до сериализации ответа API-клиентам.
         */
        public FieldViolation {
            Objects.requireNonNull(field, "field must not be null.");
            Objects.requireNonNull(message, "message must not be null.");
        }
    }
}
