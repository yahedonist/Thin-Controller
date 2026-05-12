package com.example.thincontroller.order.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain model заказа, который создает use case.
 * Здесь находятся invariants, которые должны выполняться независимо от caller:
 * controller, test, batch job или message consumer.
 */
public record Order(
        UUID id,
        String customerId,
        String productCode,
        int quantity,
        OrderStatus status
) {

    /**
     * Factory для единственного state transition в этом маленьком примере.
     * Controller не создает domain objects напрямую: use case вызывает этот
     * метод после решения, что сценарий можно выполнить.
     */
    public static Order create(UUID id, String customerId, String productCode, int quantity) {
        if (isBlank(customerId)) {
            throw new IllegalArgumentException("customerId must not be blank.");
        }
        if (isBlank(productCode)) {
            throw new IllegalArgumentException("productCode must not be blank.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1.");
        }
        return new Order(id, customerId, productCode, quantity, OrderStatus.CREATED);
    }

    /**
     * Проверяет constructor-level invariants для всех путей создания объекта.
     * Так domain correctness не зависит от HTTP DTO validation.
     */
    public Order {
        Objects.requireNonNull(id, "id must not be null.");
        Objects.requireNonNull(status, "status must not be null.");
        if (isBlank(customerId)) {
            throw new IllegalArgumentException("customerId must not be blank.");
        }
        if (isBlank(productCode)) {
            throw new IllegalArgumentException("productCode must not be blank.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1.");
        }
    }

    /**
     * Общий domain helper для invariant checks, которые должны работать
     * независимо от validation в любом HTTP adapter.
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
