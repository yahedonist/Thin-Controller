package com.example.thincontroller.order.application;

import com.example.thincontroller.order.domain.Order;
import com.example.thincontroller.order.domain.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.thincontroller.order.application.CreateOrderResult.Reason.QUANTITY_LIMIT_EXCEEDED;

/**
 * Реализация use case создания заказа.
 * Этот класс владеет business policy и repository orchestration - именно это
 * сохраняет controller тонким.
 */
@Service
public class OrderService implements CreateOrderUseCase {

    private static final int MAX_QUANTITY = 100;

    private final OrderRepository orderRepository;

    /**
     * Получает domain repository port, который нужен сценарию.
     * Controller никогда не получает эту зависимость, поэтому HTTP-код не может
     * обойти use case.
     */
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Выполняет бизнес-сценарий: применяет policy, создает domain object,
     * сохраняет его и возвращает typed result. В production версии с persistence
     * именно здесь было бы правильное место для transaction management.
     */
    @Override
    public CreateOrderResult create(CreateOrderCommand command) {
        if (command.quantity() > MAX_QUANTITY) {
            return new CreateOrderResult.QuantityLimitExceeded(QUANTITY_LIMIT_EXCEEDED);
        }

        var order = Order.create(
                UUID.randomUUID(),
                command.customerId(),
                command.productCode(),
                command.quantity()
        );
        var saved = orderRepository.save(order);

        return new CreateOrderResult.Created(
                saved.id(),
                saved.customerId(),
                saved.productCode(),
                saved.quantity(),
                saved.status()
        );
    }
}
