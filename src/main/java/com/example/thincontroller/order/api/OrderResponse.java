package com.example.thincontroller.order.api;

import java.util.UUID;

/**
 * HTTP response DTO, который возвращает controller.
 * Он отделяет публичный API contract от domain model: для Thin Controller это
 * базовое правило - маппить на границе и выполнять бизнес-работу ниже.
 */
public record OrderResponse(
        UUID id,
        String customerId,
        String productCode,
        int quantity,
        String status
) {
}
