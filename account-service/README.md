# Account Service

계좌·거래 요청을 담당하는 마이크로서비스입니다.  
클라이언트의 **입금/출금 API**를 제공하며, **OpenFeign**으로 Transaction Service를 호출하고, **Resilience4j** 서킷 브레이커로 장애 시 Fallback 응답을 반환합니다.

## 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot
- **Database**: PostgreSQL (JPA/Hibernate) — 로컬 기본 DB: `msa_db`
- **Security**: Spring Security, JWT 검증 (Auth Service JWKS 연동)
- **Client**: OpenFeign (Transaction Service 호출)
- **Resilience**: Resilience4j (Circuit Breaker, Fallback)
- **Monitoring**: Zipkin (Tracing)

## 실행 방법

### 사전 요구사항 (Infrastructure)
PostgreSQL(5432), Zipkin(9411)이 실행 중이어야 합니다. 입금/출금 API 호출 시 Auth Service에서 발급한 JWT가 필요하며, 전체 흐름 테스트 시 Auth Service(Redis 사용)도 함께 실행해야 합니다.

```bash
# 프로젝트 루트에서
docker-compose up -d
```

### 로컬 실행
**반드시 루트 디렉토리(`MSA-project`)에서** 실행하세요.

```bash
POSTGRES_PORT=5432 POSTGRES_DB=msa_db POSTGRES_USER=user POSTGRES_PASSWORD=your_password ZIPKIN_PORT=9411 REDIS_PORT=6379 JWT_SECRET=your_jwt_secret ./gradlew :account-service:bootRun
```

실행 후 `http://localhost:8080`에서 서비스가 동작합니다.

> **Note**: 모든 API는 `Authorization: Bearer <JWT>` 헤더가 필요합니다. Auth Service 로그인 후 발급받은 토큰을 사용하세요.

## API 명세

### 1. 입금 (Deposit)
- **URL**: `POST /account/deposit`
- **Header**: `Authorization: Bearer <Token>`
- **Request Body**:
  ```json
  {
    "amount": 10000
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "transactionId": 1,
    "userId": 1,
    "amount": 10000,
    "newBalance": 10000,
    "status": "SUCCESS",
    "createdAt": "..."
  }
  ```
- **Circuit Open 시 (Transaction Service 장애)**: Fallback 응답 — `status: "FAILED"`, `newBalance: 0`

### 2. 출금 (Withdrawal)
- **URL**: `POST /account/withdrawal`
- **Header**: `Authorization: Bearer <Token>`
- **Request Body**:
  ```json
  {
    "amount": 5000
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "transactionId": 2,
    "userId": 1,
    "amount": 5000,
    "newBalance": 5000,
    "status": "SUCCESS",
    "createdAt": "..."
  }
  ```
- **잔액 부족 시**: `400 Bad Request` (Transaction Service에서 처리)
- **Circuit Open 시**: Fallback — `status: "FAILED"`, `newBalance: 0`
