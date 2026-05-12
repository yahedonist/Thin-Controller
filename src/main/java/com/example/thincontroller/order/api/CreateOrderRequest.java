package com.example.thincontroller.order.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request DTO для endpoint создания заказа.
 * Bean Validation проверяет форму запроса на controller boundary, а более
 * глубокие слои все равно защищают свои invariants для не-HTTP callers.
 */
public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotBlank String productCode,
        @Min(1) int quantity
) {
}
