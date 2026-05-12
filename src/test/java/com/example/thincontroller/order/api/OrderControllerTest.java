package com.example.thincontroller.order.api;

import com.example.thincontroller.order.application.CreateOrderResult;
import com.example.thincontroller.order.application.CreateOrderUseCase;
import com.example.thincontroller.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.example.thincontroller.order.application.CreateOrderResult.Reason.QUANTITY_LIMIT_EXCEEDED;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests для HTTP adapter.
 * Они проверяют, что controller только маппит HTTP input/output и делегирует
 * все scenario decisions в use case.
 */
@WebMvcTest(OrderController.class)
@Import(ApiErrors.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOrderUseCase createOrderUseCase;

    /**
     * Доказывает happy-path pipeline controller: request DTO -> command ->
     * use case result -> 201 response с Location и response DTO.
     */
    @Test
    void createReturnsCreatedOrder() throws Exception {
        var id = UUID.randomUUID();
        when(createOrderUseCase.create(argThat(command ->
                command.customerId().equals("customer-1")
                        && command.productCode().equals("book")
                        && command.quantity() == 2
        ))).thenReturn(new CreateOrderResult.Created(id, "customer-1", "book", 2, OrderStatus.CREATED));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-1",
                                  "productCode": "book",
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.productCode").value("book"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(createOrderUseCase).create(argThat(command ->
                command.customerId().equals("customer-1")
                        && command.productCode().equals("book")
                        && command.quantity() == 2
        ));
        verifyNoMoreInteractions(createOrderUseCase);
    }

    /**
     * Доказывает, что request-shape validation остается на HTTP boundary и не
     * вызывает use case, когда body структурно невалидно.
     */
    @Test
    void createReturnsBadRequestForInvalidRequestShape() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "",
                                  "productCode": "book",
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.fields.length()").value(2));

        verifyNoMoreInteractions(createOrderUseCase);
    }

    /**
     * Доказывает, что controller маппит typed business rejection в HTTP, не
     * принимая бизнес-решение самостоятельно.
     */
    @Test
    void createMapsBusinessRejectionToUnprocessableEntity() throws Exception {
        when(createOrderUseCase.create(argThat(command -> command.quantity() == 101)))
                .thenReturn(new CreateOrderResult.QuantityLimitExceeded(QUANTITY_LIMIT_EXCEEDED));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-1",
                                  "productCode": "book",
                                  "quantity": 101
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUANTITY_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("Quantity must not be greater than 100."))
                .andExpect(jsonPath("$.fields.length()").value(0));

        verify(createOrderUseCase).create(argThat(command -> command.quantity() == 101));
        verifyNoMoreInteractions(createOrderUseCase);
    }
}
