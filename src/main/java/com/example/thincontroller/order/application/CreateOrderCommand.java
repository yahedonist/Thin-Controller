package com.example.thincontroller.order.application;

/**
 * Входная модель для use case создания заказа.
 * Здесь нет HTTP annotations, поэтому application boundary можно использовать
 * из REST, messaging, тестов, scheduler или любого будущего adapter.
 */
public record CreateOrderCommand(
        String customerId,
        String productCode,
        int quantity
) {

    /**
     * Защищает use case boundary от невалидных не-HTTP callers.
     * Controller DTO validation полезна, но Thin Controller не означает, что
     * controller становится единственной защитой бизнес-сценариев.
     */
    public CreateOrderCommand {
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
     * Локальный helper для проверки текста на command boundary без добавления
     * HTTP validation annotations в application layer.
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
