package com.example.thincontroller.order.domain;

/**
 * Domain status, который назначает use case/domain, а не controller.
 * Controller только сериализует это значение в response DTO.
 */
public enum OrderStatus {
    CREATED
}
