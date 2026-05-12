package com.example.thincontroller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.thincontroller.order.domain.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Полные Spring flow tests для единственного endpoint.
 * Они доказывают, что HTTP adapter, use case, domain и in-memory infrastructure
 * работают вместе, но ответственность каждого слоя остается отдельной.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Проверяет успешный end-to-end flow: HTTP request доходит до тонкого
     * controller, use case создает заказ, а infrastructure сохраняет его.
     */
    @Test
    void createOrderThroughHttpAndPersistInMemory() throws Exception {
        var response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-1",
                                  "productCode": "book",
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.productCode").value("book"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var id = objectMapper.readTree(response).get("id").asText();

        assertThat(orderRepository.findById(UUID.fromString(id)))
                .isPresent()
                .get()
                .satisfies(order -> {
                    assertThat(order.customerId()).isEqualTo("customer-1");
                    assertThat(order.productCode()).isEqualTo("book");
                    assertThat(order.quantity()).isEqualTo(2);
                });
    }

    /**
     * Проверяет, что невалидная форма запроса обрабатывается на HTTP boundary
     * через стабильный error contract.
     */
    @Test
    void createOrderReturnsStableBadRequestForInvalidHttpInput() throws Exception {
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
    }

    /**
     * Проверяет, что валидная форма запроса все еще может нарушить business
     * policy в use case и быть замапплена controller в стабильный 422 response.
     */
    @Test
    void createOrderReturnsStableUnprocessableEntityForBusinessRejection() throws Exception {
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
    }
}
