package com.example.thincontroller.order.api;

import com.example.thincontroller.order.application.CreateOrderCommand;
import com.example.thincontroller.order.application.CreateOrderResult;
import com.example.thincontroller.order.application.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

/**
 * Тонкий HTTP adapter для создания заказа.
 * Controller принимает request DTO, собирает command, делегирует use case и
 * маппит typed results в HTTP responses, не владея бизнес-правилами.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    /**
     * Получает application boundary вместо repository или infrastructure client,
     * чтобы controller не мог случайно выполнять бизнес-сценарий самостоятельно.
     */
    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    /**
     * Обрабатывает POST route как pipeline: request DTO -> command -> use case
     * result -> HTTP response. Единственное ветвление здесь - result-to-status
     * mapping, который в Thin Controller принадлежит HTTP boundary.
     */
    @PostMapping
    public ResponseEntity<Object> create(@Valid @RequestBody CreateOrderRequest request) {
        var command = new CreateOrderCommand(
                request.customerId(),
                request.productCode(),
                request.quantity()
        );

        return switch (createOrderUseCase.create(command)) {
            case CreateOrderResult.Created created -> ResponseEntity
                    .status(CREATED)
                    .location(URI.create("/api/orders/" + created.id()))
                    .body((Object) toResponse(created));
            case CreateOrderResult.QuantityLimitExceeded rejected -> ResponseEntity
                    .status(UNPROCESSABLE_ENTITY)
                    .body((Object) toErrorResponse(rejected));
        };
    }

    /**
     * Преобразует успешный application result в форму API response.
     * Mapper остается в controller layer, чтобы response DTO не протекали в
     * use case или domain.
     */
    private OrderResponse toResponse(CreateOrderResult.Created result) {
        return new OrderResponse(
                result.id(),
                result.customerId(),
                result.productCode(),
                result.quantity(),
                result.status().name()
        );
    }

    /**
     * Преобразует причину бизнес-отказа в стабильный API error contract.
     * Use case выбирает typed business outcome, а controller выбирает HTTP
     * status и текст для API-клиента.
     */
    private ApiErrorResponse toErrorResponse(CreateOrderResult.QuantityLimitExceeded result) {
        return ApiErrorResponse.withoutFields(
                result.reason().name(),
                "Quantity must not be greater than 100."
        );
    }
}
