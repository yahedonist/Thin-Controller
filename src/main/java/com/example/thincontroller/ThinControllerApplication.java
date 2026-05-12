package com.example.thincontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Запускает минимальное Spring Boot приложение с примером Thin Controller.
 * В этом классе намеренно нет бизнес-логики: обработка HTTP остается в
 * controller, а сценарная логика - в use case.
 */
@SpringBootApplication
public class ThinControllerApplication {

    /**
     * Поднимает Spring-контекст. Это инфраструктурная точка входа, а не часть
     * самого Thin Controller потока.
     */
    public static void main(String[] args) {
        SpringApplication.run(ThinControllerApplication.class, args);
    }
}
