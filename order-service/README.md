# Order Service

주문 처리를 담당하는 마이크로서비스입니다.

## 기술 스택
- Java 17
- Spring Boot 3.5.10
- PostgresSQL (Data Persistence)
- Zipkin (Distributed Tracing)
- SpringDoc OpenAPI (Swagger)
- Resilience4j (Circuit Breaker)
- Spring Security + JWT (Authentication)

## API 명세

### 주문 생성
- **URL**: `POST /order`
- **Description**: 새로운 주문을 생성합니다. (추후 결제 서비스와 연동 예정)
- **Request Body (OrderRequest)**:
    ```json
    {
      "userId": 12345,
      "productId": 101,
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 1500000,
      "paymentMethod": "CREDIT_CARD" // [CREDIT_CARD, CASH, EASY_PAYMENT]
    }
    ```
- **Response (OrderResponse)**: `201 Created`
    ```json
    {
      "orderId": 1,
      "userId": 12345,
      "productId": 101,
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 1500000,
      "totalAmount": 1500000,
      "status": "PENDING",
      "createdAt": "2024-01-24T12:00:00"
    }
    ```

## 구조
- **Controller**: `OrderController` - API 진입점
- **Service**: `OrderService` - 비즈니스 로직
- **Domain**: `Order` (Entity), `OrderStatus` (Enum)
- **Repository**: `OrderRepository` - JPA 데이터 접근

## 실행 방법
```bash
# 로컬 실행 (의존성: Docker로 Postgres, Zipkin 실행 필요)
./gradlew bootRun
```
- Swagger UI: http://localhost:8080/swagger-ui/index.html
