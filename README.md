# MSA Project (FISA-L2J)

Spring Boot 기반의 마이크로서비스 아키텍처(MSA) 이커머스 데모 프로젝트입니다.  
주문(Order), 결제(Payment), 인증(Auth) 서비스로 구성되어 있으며, 서비스 간 통신, 장애 격리, 분산 트레이싱 등 MSA의 핵심 패턴들을 구현했습니다.

## 🏗 아키텍처 및 기술 스택

### Infrastructure
- **RDBMS**: PostgreSQL (각 서비스별 Database 분리)
- **Cache**: Redis (Auth Service 토큰 관리)
- **Tracing**: Zipkin (분산 트레이싱 시각화)
- **Container**: Docker (DB 및 인프라 실행 위주)

### Microservices
| 서비스 | 기술 스택 | 주요 역할 | 포트 |
| --- | --- | --- | --- |
| **Auth Service** | Spring Security, JWT, Redis | 사용자 가입/로그인/로그아웃, 토큰 발급 및 검증 | 8082 |
| **Order Service** | Spring Boot, OpenFeign, Resilience4j | 주문 생성, 결제 요청(Client), 서킷 브레이커 | 8080 |
| **Payment Service** | Spring Boot, JPA | 결제 승인/거절 처리 | 8081 |

---

## 🌟 핵심 기능 (Key Features)

### 1. Token Propagation (토큰 전파)
- **FeignClientInterceptor**를 통해 `Order Service`로 들어온 요청의 JWT 토큰을 추출하여, 내부적으로 호출하는 `Payment Service`로 전달합니다.
- 이를 통해 마이크로서비스 간의 호출에서도 **사용자 인증 정보(User Context)가 끊기지 않고 유지**됩니다.

### 2. Circuit Breaker (서킷 브레이커)
- **Resilience4j**를 적용하여 `Payment Service` 장애 시 `Order Service`가 영향을 받지 않도록 격리합니다.
- **Fail Fast**: 장애 감지 시 즉시 에러(또는 Fallback)를 반환하여 스레드 고갈을 방지합니다.
- **Fallback**: 결제 서비스 다운 시, 주문을 '실패(FAILED)' 상태로 기록하되 시스템 오류(500)가 아닌 정상 응답으로 처리합니다.

---

## 📊 모니터링 (Monitoring)

### Zipkin Dashboard
- **URL**: `http://localhost:9411`
- 분산 트레이싱을 통해 서비스 간의 호출 흐름과 지연 시간, **서킷 브레이커 동작(Error/Short Duration)** 을 시각적으로 확인할 수 있습니다.

---

## 📚 API 명세서 (API Documentation)

### 1. Auth Service (Port: 8082)
사용자 인증 및 JWT 토큰 관리

#### 회원가입
- **URL**: `POST /auth/signup`
- **Request**:
  ```json
  {
    "username": "user",
    "password": "password"
  }
  ```
- **Response**: `200 OK` ("User registered successfully") 

#### 로그인
- **URL**: `POST /auth/login`
- **Request**:
  ```json
  {
    "username": "user",
    "password": "password"
  }
  ```
- **Response**: `200 OK` (Token Return)

#### 로그아웃
- **URL**: `POST /auth/logout`
- **Header**: `Authorization: Bearer <Token>`
- **Description**: 토큰을 Redis 블랙리스트에 등록하여 남은 유효기간 동안 무효화

#### 토큰 검증
- **URL**: `GET /auth/validate`
- **Query Param**: `?token={accessToken}`
- **Response**: `200 OK` (Valid), `401 Unauthorized` (Invalid/Blacklisted)

---

### 2. Order Service (Port: 8080)
주문 관리 및 결제 서비스 호출 (Requires JWT Authentication)

> **Note**: 모든 요청의 Header에 `Authorization: Bearer <Token>`이 필요합니다.

#### 주문 생성
- **URL**: `POST /order`
- **Request**:
  ```json
  {
    "productId": 101,
    "productName": "Laptop",
    "quantity": 1,
    "unitPrice": 1500000,
    "paymentMethod": "CREDIT_CARD" // [CREDIT_CARD, CASH, EASY_PAYMENT]
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "orderId": 1,
    "userId": 1,
    "status": "COMPLETED", // 결제 성공 시
    "totalAmount": 1500000,
    "createdAt": "..."
  }
  ```

---

### 3. Payment Service (Port: 8081)
결제 처리 (일반적으로 내부 서비스에서 호출됨)

#### 결제 승인
- **URL**: `POST /payment/process`
- **Request**:
  ```json
  {
    "orderId": 1,
    "userId": 1,
    "amount": 1500000,
    "paymentMethod": "CREDIT_CARD"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "paymentId": 1,
    "status": "SUCCESS",
    "orderId": 1
  }
  ```

---

## 🚀 실행 방법 (Getting Started)

### 1. 인프라 실행 (Docker)
프로젝트 루트에서 `docker-compose`를 사용하여 인프라를 실행합니다.

```bash
docker-compose up -d
docker ps
# Postgres(5432), Zipkin(9411), Redis(6379) 확인
```

### 2. 서비스 실행
 **중요**: 각 서비스는 루트 디렉토리(`MSA-project`)에서 아래 명령어로 실행해야 합니다. (환경변수 포함)

#### Auth Service
```bash
POSTGRES_PORT=5432 POSTGRES_DB=msa_db POSTGRES_USER=user POSTGRES_PASSWORD=41cc57bf7f1a8f4db0941c8bc842be8cb7c1f71c945c2bb7bcc523e262aef71b ZIPKIN_PORT=9411 REDIS_PORT=6379 JWT_SECRET=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437 ./gradlew :auth-service:bootRun
```

#### Payment Service
```bash
POSTGRES_PORT=5432 POSTGRES_DB=msa_db POSTGRES_USER=user POSTGRES_PASSWORD=41cc57bf7f1a8f4db0941c8bc842be8cb7c1f71c945c2bb7bcc523e262aef71b ZIPKIN_PORT=9411 REDIS_PORT=6379 JWT_SECRET=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437 ./gradlew :payment-service:bootRun
```

#### Order Service
```bash
POSTGRES_PORT=5432 POSTGRES_DB=msa_db POSTGRES_USER=user POSTGRES_PASSWORD=41cc57bf7f1a8f4db0941c8bc842be8cb7c1f71c945c2bb7bcc523e262aef71b ZIPKIN_PORT=9411 REDIS_PORT=6379 JWT_SECRET=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437 ./gradlew :order-service:bootRun
```
*모든 서비스를 띄워야 전체 흐름 테스트가 가능합니다.*

### 3. 전체 흐름 테스트 (Scenario)
1. **Auth**: `POST /auth/login`으로 토큰 획득 (userId=1)
2. **Order**: 획득한 토큰을 헤더에 넣고 `POST /order` 요청
3. **Logout**: `POST /auth/logout`으로 토큰 무효화
3. **Verify**: 무효화된 토큰으로 API 호출 시 `401` 에러 확인