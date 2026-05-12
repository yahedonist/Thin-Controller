package com.example.thincontroller.order.application;

import com.example.thincontroller.order.domain.OrderStatus;

import java.util.UUID;

/**
 * Типизированный результат use case создания заказа.
 * Application layer возвращает business outcomes без импортов ResponseEntity,
 * status codes, JSON и других HTTP-концепций.
 */
public sealed interface CreateOrderResult permits CreateOrderResult.Created, CreateOrderResult.QuantityLimitExceeded {

    /**
     * Успешный business outcome с данными, которые controller может замаппить
     * в HTTP response 201.
     */
    record Created(
            UUID id,
            String customerId,
            String productCode,
            int quantity,
            OrderStatus status
    ) implements CreateOrderResult {
    }

    /**
     * Business rejection outcome для command, который валиден по форме, но
     * нарушает quantity policy внутри use case.
     */
    record QuantityLimitExceeded(Reason reason) implements CreateOrderResult {
    }

    /**
     * Machine-readable причина отказа. Controller маппит ее в API text и status
     * code, сохраняя бизнес-смысл отдельно от transport details.
     */
    enum Reason {
        QUANTITY_LIMIT_EXCEEDED
    }
}
