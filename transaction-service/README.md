# Transaction Service

잔액·거래 처리를 담당하는 마이크로서비스입니다.  
**입금/출금 실행**과 **잔액(Balance)** 갱신을 수행하며, 일반적으로 Account Service에서 내부 호출됩니다. 잔액 갱신 시 **JPA 낙관적 락(@Version)** 으로 동시 요청 시 정합성을 유지합니다.

## 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot
- **Database**: PostgreSQL (JPA/Hibernate) — 로컬 기본 DB: `msa_db`
- **Monitoring**: Zipkin (Tracing)
- **API Docs**: Springdoc OpenAPI (Swagger UI — `/swagger-ui.html`)

## 실행 방법

### 사전 요구사항 (Infrastructure)
PostgreSQL(5432), Zipkin(9411)이 실행 중이어야 합니다.

```bash
# 프로젝트 루트에서
docker-compose up -d
```

### 로컬 실행
**반드시 루트 디렉토리(`MSA-project`)에서** 실행하세요.

```bash
POSTGRES_PORT=5432 POSTGRES_DB=msa_db POSTGRES_USER=user POSTGRES_PASSWORD=your_password ZIPKIN_PORT=9411 ./gradlew :transaction-service:bootRun
```

실행 후 `http://localhost:8081`에서 서비스가 동작합니다.

> **Note**: 이 서비스는 주로 Account Service가 Feign으로 호출합니다. 직접 호출 시 `userId`, `amount`를 Request Body에 포함하세요.

## API 명세

### 1. 입금 처리 (내부)
- **URL**: `POST /transaction/deposit`
- **Request Body**:
  ```json
  {
    "userId": 1,
    "amount": 10000
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "transactionId": 1,
    "newBalance": 10000,
    "status": "SUCCESS",
    "createdAt": "..."
  }
  ```

### 2. 출금 처리 (내부)
- **URL**: `POST /transaction/withdrawal`
- **Request Body**:
  ```json
  {
    "userId": 1,
    "amount": 5000
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "transactionId": 2,
    "newBalance": 5000,
    "status": "SUCCESS",
    "createdAt": "..."
  }
  ```
- **잔액 부족 시**: `400 Bad Request` — 본문 예: `"Insufficient balance. Current: 1000, Requested: 5000"`
