# Payment Service

결제 처리를 담당하는 마이크로서비스입니다.

## 기술 스택
- Java 17
- Spring Boot 3.5.10
- PostgresSQL (Data Persistence)
- Zipkin (Distributed Tracing)
- SpringDoc OpenAPI (Swagger)

## API 명세

### 결제 승인
- **URL**: `POST /payment/process`
- **Description**: 주문에 대한 결제를 수행하고 결과를 반환합니다.
- **Request Body (PaymentRequest)**:
    ```json
    {
      "orderId": 12345,
      "userId": 67890,
      "amount": 50000,
      "paymentMethod": "CREDIT_CARD" // [CREDIT_CARD, CASH, EASY_PAYMENT]
    }
    ```
- **Response (PaymentResponse)**: `201 Created`
    ```json
    {
      "paymentId": 1,
      "status": "SUCCESS", // [PENDING, SUCCESS, FAIL]
      "amount": 50000,
      "orderId": 12345,
      "createdAt": "2024-01-24T12:00:00"
    }
    ```

## 구조
- **Controller**: `PaymentController` - API 진입점
- **Service**: `PaymentService` - 비즈니스 로직 (결제 생성 등)
- **Domain**: `Payment` (Entity), `PaymentMethod` (Enum), `PaymentStatus` (Enum)
- **Repository**: `PaymentRepository` - JPA 데이터 접근

## 실행 방법
```bash
# 로컬 실행 (의존성: Docker로 Postgres, Zipkin 실행 필요)
./gradlew bootRun
```
- Swagger UI: http://localhost:8081/swagger-ui/index.html
