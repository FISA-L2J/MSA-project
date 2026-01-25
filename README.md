# MSA Project (FISA-L2J)

Spring Boot 기반의 마이크로서비스 아키텍처(MSA) 이커머스 데모 프로젝트입니다.  
주문(Order), 결제(Payment), 인증(Auth) 서비스로 구성되어 있으며, 서비스 간 통신, 장애 격리, 분산 트레이싱 등 MSA의 핵심 패턴들을 구현했습니다.

## 🏗 아키텍처 및 기술 스택

### Infrastructure
- **Cloud**: Google Cloud Platform (Compute Engine, Artifact Registry)
- **IaC**: Terraform (인프라 자동 프로비저닝)
- **CI/CD**: GitHub Actions (자동 빌드 및 배포)
- **RDBMS**: PostgreSQL (각 서비스별 Database 분리)
- **Cache**: Redis (Auth Service 토큰 관리)
- **Tracing**: Zipkin (분산 트레이싱 시각화)
- **Container**: Docker & Docker Compose

### Microservices
| 서비스 | 기술 스택 | 주요 역할 | 포트 |
| --- | --- | --- | --- |
| **Auth Service** | Spring Security, JWT, Redis | 사용자 가입/로그인/로그아웃, 토큰 발급 및 검증 | 8082 |
| **Order Service** | Spring Boot, OpenFeign, Resilience4j | 주문 생성, 결제 요청(Client), 서킷 브레이커 | 8080 |
| **Payment Service** | Spring Boot, JPA | 결제 승인/거절 처리 | 8081 |

---

## 🚀 배포 및 자동화 (Deployment & Automation)

이 프로젝트는 **Terraform**으로 인프라를 생성하고, **GitHub Actions**로 자동 배포(CD)를 수행합니다.

### 1. 인프라 생성 (Terraform)
`/terraform` 디렉토리에서 GCP 리소스를 생성합니다.
- **Artifact Registry**: Docker 이미지 저장소 (`msa-repo`)
- **Compute Engine**: Docker가 설치된 VM (`msa-server`)
- **Firewall**: 8080-8082, 9411 포트 개방

```bash
cd terraform
# 초기화
terraform init
# 생성 (GCP 인증 필요)
terraform apply
```

### 2. CI/CD 파이프라인 (GitHub Actions)
- **CI (`*-service-ci.yml`)**:
  - `main` 브랜치에 푸시되면 각 서비스별로 빌드 및 테스트를 수행합니다.
  - Docker 이미지를 빌드하여 GCP Artifact Registry에 업로드합니다.
- **CD (`deploy.yml`)**:
  - **수동 실행 (Workflow Dispatch)** 방식입니다.
  - VM에 SSH로 접속하여 최신 이미지를 받아오고(`docker compose pull`), 컨테이너를 재시작(`up -d`)합니다.
  - 실행 시 GitHub Secrets에 저장된 환경변수(`db password` 등)를 안전하게 주입합니다.

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
- **URL**: `http://<VM-Public-IP>:9411`
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

## 🚀 로컬 실행 방법 (Local Development)

### 1. 인프라 실행 (Docker)
프로젝트 루트에서 `docker-compose`를 사용하여 로컬 DB 등을 실행합니다.

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

---

## 🛠️ 트러블 슈팅 (Troubleshooting)

### 1. Cloud & Infrastructure (GCP, Terraform)

#### 🔴 GCP Permission Denied (403)
- **Issue**: `Artifact Registry` 리소스 생성 중 403 Forbidden 에러.
- **Cause**: 서비스 계정에 `Compute Admin` 권한은 있었으나, `Artifact Registry Administrator` 권한이 누락됨.
- **Solution**: GCP Console IAM 설정에서 서비스 계정에 **Artifact Registry 관리자** 역할 추가.

### 2. Version Control (Git & GitHub)

#### 🔴 Large File Push Error
- **Issue**: `git push` 시 `.terraform` 폴더 내의 바이너리 파일(100MB+)로 인해 푸시 거부됨.
- **Cause**: `.gitignore`에 Terraform 관련 설정이 없어서 로컬 바이너리가 커밋됨.
- **Solution**:
  1. `.gitignore`에 `.terraform/`, `*.tfstate` 등 추가.
  2. `git reset HEAD^`로 커밋 취소 후 다시 스테이징(`git add`) 및 커밋.

#### 🔴 Personal Access Token (PAT) Scope
- **Issue**: `refusing to allow a Personal Access Token to create or update workflow` 에러.
- **Cause**: GitHub 인증 토큰에 `workflow` 스코프(권한)가 비활성화됨.
- **Solution**: GitHub Developer Settings에서 토큰의 **Scopes**를 수정하여 `workflow` 항목 체크.

### 3. DevOps (Docker & CI/CD)

#### 🔴 Docker Build Context
- **Issue**: 로컬용 `docker-compose.yml`은 `build: context`를 사용하므로 소스 코드가 없는 프로덕션 환경(VM)에서 실행 불가.
- **Solution**: CI 파이프라인에서 빌드한 이미지를 레지스트리(GCR)에 올리고, `docker-compose.yml`은 이미지를 당겨오도록(`image: ...`) 수정.

### 4. Application Verification (Runtime & Logic)

#### 🔴 Build Configuration - Redundant Plugin
- **Issue**: `Order Service` 실행 시 빌드 실패.
- **Cause**: 루트 프로젝트(`build.gradle`)의 `subprojects` 블록과 각 서비스의 `build.gradle`에 동일한 플러그인(`java`, `org.springframework.boot`)이 중복 선언됨.
- **Solution**: 하위 모듈의 `build.gradle`에서 중복되는 플러그인 선언 제거.

#### 🔴 Build Configuration - Version Mismatch
- **Issue**: `Spring Boot 3.5.10` 버전 사용 시 `Spring Cloud`와의 호환성 문제로 빌드 실패.
- **Cause**: `Spring Cloud` 릴리즈 트레인과 호환되지 않는 `Spring Boot` 버전을 사용하여 의존성 충돌 발생.
- **Solution**: 호환성이 보장된 `Spring Boot 3.4.1`로 버전을 다운그레이드하여 해결.

#### 🔴 Execution Context - Gradle Wrapper
- **Issue**: `auth-service` 디렉토리 내부에서 `./gradlew bootRun` 실행 시 빌드 실패.
- **Cause**: 루트 프로젝트에 정의된 공통 설정(플러그인, 의존성 등)을 읽지 못하고 서브 모듈을 독립 프로젝트로 인식함.
- **Solution**: 반드시 **루트 디렉토리(`MSA-project`)** 에서 `:auth-service:bootRun` 형태로 실행하도록 가이드 수정.

#### 🔴 Runtime - Missing Environment Variables
- **Issue**: 애플리케이션 실행 중 `InjectionMetadata` 관련 에러 발생.
- **Cause**: `JWT_SECRET`, `POSTGRES_USER` 등 필수 환경변수가 터미널 세션에 설정되지 않음.
- **Solution**: 실행 명령어에 필요한 모든 환경변수(`export ...`)를 포함하여 한 줄로 실행하도록 스크립트 제공.

#### 🔴 Logic - Missing Endpoint & Malformed Token
- **Issue 1**: 회원가입 요청 시 `404 Not Found`.
  - **Cause**: `Auth Service`에 `/auth/signup` 엔드포인트가 아예 구현되어 있지 않았음.
  - **Solution**: `AuthService` 및 `AuthController`에 회원가입 로직 추가 구현.
- **Issue 2**: 주문 요청 시 `403 Forbidden`.
  - **Cause**: `Authorization` 헤더에 JWT 토큰 문자열만 넣어야 하는데, JSON 응답 전체(`{"accessToken":...}`)를 넣음.
  - **Solution**: `curl` 및 `python` 파싱을 통해 `accessToken` 값만 정확히 추출하여 헤더에 주입.
