package com.example.thincontroller.order.application;

import com.example.thincontroller.order.domain.Order;
import com.example.thincontroller.order.domain.OrderRepository;
import com.example.thincontroller.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.thincontroller.order.application.CreateOrderResult.Reason.QUANTITY_LIMIT_EXCEEDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests для use case layer.
 * Они держат покрытие бизнес-правил ниже controller - в этом и смысл паттерна
 * Thin Controller.
 */
class OrderServiceTest {

    /**
     * Проверяет, что use case выполняет сценарную работу: создает domain object,
     * сохраняет его через repository port и возвращает typed success.
     */
    @Test
    void createSavesOrderAndReturnsCreatedResult() {
        var repository = new RecordingOrderRepository();
        var service = new OrderService(repository);

        var result = service.create(new CreateOrderCommand("customer-1", "book", 2));

        assertThat(result).isInstanceOf(CreateOrderResult.Created.class);
        var created = (CreateOrderResult.Created) result;
        assertThat(created.id()).isNotNull();
        assertThat(created.customerId()).isEqualTo("customer-1");
        assertThat(created.productCode()).isEqualTo("book");
        assertThat(created.quantity()).isEqualTo(2);
        assertThat(created.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(repository.savedOrder())
                .isPresent()
                .get()
                .extracting(Order::id, Order::status)
                .containsExactly(created.id(), OrderStatus.CREATED);
    }

    /**
     * Проверяет, что quantity policy применяется в use case, а не в controller.
     */
    @Test
    void createRejectsQuantityGreaterThanBusinessLimit() {
        var repository = new RecordingOrderRepository();
        var service = new OrderService(repository);

        var result = service.create(new CreateOrderCommand("customer-1", "book", 101));

        assertThat(result)
                .isEqualTo(new CreateOrderResult.QuantityLimitExceeded(QUANTITY_LIMIT_EXCEEDED));
        assertThat(repository.savedOrder()).isEmpty();
    }

    /**
     * Проверяет, что application command защищает use case от невалидных
     * non-HTTP callers.
     */
    @Test
    void commandRejectsInvalidApplicationBoundaryInput() {
        assertThatThrownBy(() -> new CreateOrderCommand("", "book", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerId must not be blank.");
        assertThatThrownBy(() -> new CreateOrderCommand("customer-1", "", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productCode must not be blank.");
        assertThatThrownBy(() -> new CreateOrderCommand("customer-1", "book", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be at least 1.");
    }

    /**
     * Проверяет, что domain invariants не зависят от controller или DTO
     * validation.
     */
    @Test
    void domainRejectsInvalidOrderState() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), "", "book", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerId must not be blank.");
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), "customer-1", "", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productCode must not be blank.");
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), "customer-1", "book", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be at least 1.");
    }

    /**
     * Тестовый repository adapter, который записывает вызовы без добавления
     * Spring или infrastructure в use case tests.
     */
    private static final class RecordingOrderRepository implements OrderRepository {

        private final AtomicReference<Order> savedOrder = new AtomicReference<>();

        /**
         * Запоминает заказ, чтобы тест мог проверить: use case сохранил domain
         * object после применения бизнес-правил.
         */
        @Override
        public Order save(Order order) {
            savedOrder.set(order);
            return order;
        }

        /**
         * Реализует repository port для полноты тестового double без real
         * storage в unit test.
         */
        @Override
        public Optional<Order> findById(UUID id) {
            return savedOrder()
                    .filter(order -> order.id().equals(id));
        }

        /**
         * Отдает сохраненный заказ для assertions без утечки этого helper в
         * production code.
         */
        Optional<Order> savedOrder() {
            return Optional.ofNullable(savedOrder.get());
        }
    }
}
