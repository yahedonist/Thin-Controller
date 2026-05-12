package com.example.thincontroller.order.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository port, который использует application service.
 * Interface не дает use case зависеть от storage implementation и не дает
 * controller работать со storage напрямую.
 */
public interface OrderRepository {

    /**
     * Сохраняет заказ после того, как use case принял бизнес-сценарий.
     */
    Order save(Order order);

    /**
     * Ищет заказ по id для тестов и будущих read use cases, не раскрывая
     * infrastructure details верхним слоям.
     */
    Optional<Order> findById(UUID id);
}
