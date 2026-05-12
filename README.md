# Thin Controller Demo

Учебный Spring Boot проект, который показывает архитектурный паттерн **Thin Controller** на примере REST API для создания заказа.

Проект намеренно небольшой: в нем нет базы данных, внешних интеграций и сложной инфраструктуры. Хранение реализовано через in-memory repository, чтобы внимание оставалось на разделении ответственностей между HTTP-слоем, application use case, domain model и infrastructure adapter.

## Что делает проект

Приложение предоставляет endpoint для создания заказа:

```http
POST /api/orders
```

Клиент отправляет данные заказа: идентификатор покупателя, код продукта и количество. Приложение валидирует форму HTTP-запроса, выполняет бизнес-сценарий создания заказа, сохраняет заказ в памяти и возвращает результат через стабильный API contract.

Основной учебный сценарий:

1. HTTP-клиент отправляет JSON-запрос.
2. Controller проверяет request DTO через Bean Validation.
3. Controller преобразует DTO в application command.
4. Use case применяет бизнес-правила.
5. Domain model защищает свои инварианты.
6. Repository adapter сохраняет заказ в памяти.
7. Controller преобразует typed result в HTTP response.

## Какой паттерн используется

В проекте используется паттерн **Thin Controller**.

Thin Controller означает, что controller остается тонким HTTP adapter. Он не содержит бизнес-правил, не работает напрямую с repository и не принимает сценарные решения. Его задача - связать внешний HTTP contract с application layer.

В этом проекте `OrderController` делает только boundary-работу:

- принимает HTTP-запрос;
- запускает Bean Validation для request DTO;
- собирает `CreateOrderCommand`;
- вызывает `CreateOrderUseCase`;
- маппит результат use case в HTTP status, headers и response body.

Бизнес-правило `quantity <= 100` находится не в controller, а в `OrderService`. Это важная часть паттерна: если тот же сценарий позже будет вызван не из HTTP, а из batch job, message consumer или CLI, бизнес-правило останется в одном месте.

## Архитектура проекта

Код разделен на несколько слоев внутри package `com.example.thincontroller.order`.

### `api`

HTTP-слой приложения.

Содержит:

- `OrderController` - тонкий REST controller;
- `CreateOrderRequest` - request DTO с Bean Validation annotations;
- `OrderResponse` - response DTO для успешного создания заказа;
- `ApiErrorResponse` - стабильный формат ошибки;
- `ApiErrors` - централизованный mapping framework validation errors в HTTP response.

Этот слой знает про HTTP, JSON, status codes и DTO. Он не должен владеть бизнес-сценарием.

### `application`

Слой use case.

Содержит:

- `CreateOrderUseCase` - application boundary;
- `OrderService` - реализация сценария создания заказа;
- `CreateOrderCommand` - входная команда use case;
- `CreateOrderResult` - typed result успешного создания или бизнес-отказа.

Здесь находится orchestration сценария: проверка бизнес-лимита, создание domain object и сохранение через repository port.

### `domain`

Доменный слой.

Содержит:

- `Order` - domain model заказа;
- `OrderStatus` - статус заказа;
- `OrderRepository` - repository port.

`Order` защищает инварианты домена: `customerId` и `productCode` не должны быть пустыми, `quantity` должен быть не меньше `1`. Эти проверки не зависят от HTTP validation, поэтому domain model остается корректной при любом caller.

### `infrastructure`

Инфраструктурный adapter.

Содержит:

- `InMemoryOrderRepository` - in-memory реализацию `OrderRepository`.

Этот adapter нужен для запуска примера без базы данных. Замена на JPA, JDBC или другой storage не должна менять controller.

## API

### Создать заказ

```http
POST /api/orders
Content-Type: application/json
```

Request body:

```json
{
  "customerId": "customer-1",
  "productCode": "book",
  "quantity": 2
}
```

Успешный ответ:

```http
201 Created
Location: /api/orders/{id}
```

```json
{
  "id": "4a8b5f6d-4d2e-4c6a-9c3f-0f4d5a9b8c7e",
  "customerId": "customer-1",
  "productCode": "book",
  "quantity": 2,
  "status": "CREATED"
}
```

### Ошибка валидации HTTP-запроса

Если request body нарушает DTO validation, например `customerId` пустой или `quantity` меньше `1`, API возвращает:

```http
400 Bad Request
```

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "fields": [
    {
      "field": "customerId",
      "message": "must not be blank"
    }
  ]
}
```

### Бизнес-отказ

Если форма запроса валидна, но количество превышает бизнес-лимит `100`, use case возвращает typed business rejection, а controller маппит его в:

```http
422 Unprocessable Entity
```

```json
{
  "code": "QUANTITY_LIMIT_EXCEEDED",
  "message": "Quantity must not be greater than 100.",
  "fields": []
}
```

## Как запустить

Требования:

- Java 21+
- Maven

В проекте нет Maven Wrapper (`mvnw`), поэтому Maven должен быть установлен в системе.

Запуск приложения:

```bash
mvn spring-boot:run
```

После запуска endpoint будет доступен по адресу:

```text
http://localhost:8080/api/orders
```

Пример запроса через `curl`:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "customer-1",
    "productCode": "book",
    "quantity": 2
  }'
```

## Как проверить

Запуск тестов:

```bash
mvn test
```

Тесты показывают паттерн с разных сторон:

- `OrderControllerTest` проверяет, что controller только маппит HTTP input/output и делегирует use case;
- `OrderServiceTest` проверяет бизнес-правила и работу application layer без Spring MVC;
- `OrderFlowTest` проверяет полный flow от HTTP-запроса до сохранения заказа в in-memory repository.

Если команда `mvn test` не запускается с ошибкой `command not found: mvn`, это означает, что Maven не установлен или не доступен в `PATH`. Это проблема окружения, а не ошибка проекта.

## Главная идея

Thin Controller помогает не превращать REST controller в место, где смешаны HTTP, validation, business rules, transactions и persistence. В этом проекте controller остается адаптером, use case владеет сценарием, domain model защищает правила состояния, а infrastructure отвечает только за техническое хранение данных.

Такое разделение упрощает тестирование, снижает связность и делает код готовым к расширению: можно заменить in-memory repository на реальную базу данных или добавить другой входной adapter, не переписывая бизнес-сценарий.
