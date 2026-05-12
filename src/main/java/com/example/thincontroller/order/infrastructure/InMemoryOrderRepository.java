package com.example.thincontroller.order.infrastructure;

import com.example.thincontroller.order.domain.Order;
import com.example.thincontroller.order.domain.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory adapter для repository port.
 * Он нужен только для запуска примера без шума от database; замена на JPA/JDBC
 * не изменила бы Thin Controller boundary.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

    /**
     * Сохраняет уже валидный domain object.
     * Infrastructure хранит состояние, но не принимает бизнес-решения.
     */
    @Override
    public Order save(Order order) {
        orders.put(order.id(), order);
        return order;
    }

    /**
     * Читает in-memory состояние без утечки storage details за пределы adapter.
     */
    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }
}
