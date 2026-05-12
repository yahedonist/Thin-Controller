package com.example.thincontroller.order.application;

/**
 * Application boundary для одного бизнес-сценария.
 * Controllers зависят от этого interface вместо repositories, поэтому HTTP
 * layer делегирует workflow decisions в use case.
 */
public interface CreateOrderUseCase {

    /**
     * Запускает сценарий создания заказа и возвращает typed business result.
     * В реальной persistent реализации transaction management находился бы
     * здесь: ниже HTTP и выше repository calls.
     */
    CreateOrderResult create(CreateOrderCommand command);
}
