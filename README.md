# MSA Project (FISA-L2J) - Cloud Native Migration

Spring Boot 기반의 마이크로서비스 아키텍처(MSA) 이커머스 데모 프로젝트입니다.  
기존 VM 기반 배포에서 **Kubernetes(GKE) 및 Istio Service Mesh** 환경으로 마이그레이션되었습니다.

주문(Order), 결제(Payment), 인증(Auth) 서비스로 구성되어 있으며, 서비스 간 통신, 장애 격리, 분산 트레이싱 등 MSA의 핵심 패턴들을 구현했습니다.

## 🏗 아키텍처 및 기술 스택

### Infrastructure (Cloud Native)
- **Cloud**: Google Cloud Platform (GKE Standard Cluster, Artifact Registry)
- **IaC**: Terraform (GKE Cluster & Node Pool 프로비저닝)
- **Service Mesh**: Istio (Traffic Management, Ingress Gateway)
- **CI/CD**: GitHub Actions (Docker Build -> Artifact Registry -> GKE Deploy)
- **RDBMS**: PostgreSQL (GKE 내 StatefulSet, Logical DB 분리: `auth_db`, `order_db`, `payment_db`)
- **Cache**: Redis (Auth Service 토큰 관리)
- **Tracing**: Zipkin (분산 트레이싱)

### Microservices
| 서비스 | 기술 스택 | 주요 역할 | 포트 |
| --- | --- | --- | --- |
| **Auth Service** | Spring Security, JWT(RS256), Redis | 사용자 가입/로그인/로그아웃, JWKS 공개키 제공 | 8082 |
| **Order Service** | Spring Boot, OpenFeign, Resilience4j | 주문 생성, 결제 요청(Client), 서킷 브레이커 | 8080 |
| **Payment Service** | Spring Boot, JPA | 결제 승인/거절 처리 | 8081 |

---

## 🚀 Cloud Native 배포 가이드 (GKE & Istio)

이 프로젝트는 **Terraform**으로 GKE 클러스터를 생성하고, **GitHub Actions**로 자동 배포(CD)를 수행합니다.

### 0. 사전 요구사항 (Prerequisites)
로컬 또는 **Google Cloud Shell**(추천)에 다음 도구가 설치되어 있어야 합니다.
- `gcloud` CLI
- `kubectl`
- `istioctl`
- `terraform`

### 1. 인프라 생성 (Terraform)
`/terraform` 디렉토리에서 GKE 클러스터를 생성합니다. (기존 VM은 삭제됩니다)

```bash
cd terraform
# 초기화
terraform init
# 생성 (GCP 인증 필요)
terraform apply
# 완료 후 출력되는 'get_credentials_command'를 실행하여 kubectl을 연결하세요.
# 예: gcloud container clusters get-credentials msa-cluster ...
```

### 2. Istio 설치 (Manual Step)
클러스터 생성 후, Istio를 수동으로 설치해야 합니다.

```bash
# Istio 다운로드 및 설치
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH
istioctl install --set profile=demo -y
```

### 3. 애플리케이션 배포 (GitHub Actions)
코드를 `main` 브랜치에 Push하면 GitHub Actions(`deploy.yml`)가 자동으로 실행됩니다.
1. Docker 이미지 빌드 및 Artifact Registry 푸시
2. GKE에 Kubernetes Manifests(`k8s/`) 배포 (Secret 자동 생성 포함)
3. `Istio Gateway` 및 `VirtualService` 설정

### 4. 접속 확인 및 모니터링
Istio Ingress Gateway의 External IP를 확인하여 접속합니다.

```bash
kubectl get svc istio-ingressgateway -n istio-system
# EXTERNAL-IP 확인 후: http://<EXTERNAL-IP>/orders
```

**Kiali 대시보드 (Service Mesh 시각화)**:
```bash
istioctl dashboard kiali
```

---

## 🌟 핵심 기능 (Key Features)

### 1. Istio Service Mesh
- **Traffic Management**: `Istio Gateway`를 통해 모든 외부 트래픽을 단일 진입점으로 관리합니다.
- **Sidecar Proxy**: 각 서비스 파드에 Envoy 프록시가 주입되어 트래픽을 가로채고 제어합니다.

### 2. Token Propagation (토큰 전파)
- **FeignClientInterceptor**를 통해 `Order Service`로 들어온 요청의 JWT 토큰을 추출하여, 내부적으로 호출하는 `Payment Service`로 전달합니다.
- 이를 통해 마이크로서비스 간의 호출에서도 **사용자 인증 정보(User Context)가 끊기지 않고 유지**됩니다.

### 3. Resilience (회복 탄력성)
- **Circuit Breaker**: `Payment Service` 장애 시 `Order Service`의 **Resilience4j**가 동작하여 장애 전파를 차단합니다. Order Service는 Fallback 응답을 반환하여 시스템 전체 중단을 방지합니다.

### 4. Database Isolation
- 단일 PostgreSQL 파드 내에서 `auth_db`, `order_db`, `payment_db`로 논리적 분리를 구현했습니다. (Database-per-service 패턴 준수)
- `k8s/secret.yaml`을 통해 DB 자격증명을 안전하게 관리합니다.

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

### 1. Cloud & Infrastructure (GKE, Terraform)

#### 🔴 GCP IAM Permission Denied (403)
- **Issue**: `Artifact Registry` 리소스 생성 중 403 Forbidden 에러.
- **Solution**: 서비스 계정에 **Artifact Registry 관리자**(이미지 푸시용) 및 **Kubernetes Engine 관리자**(클러스터 생성용) 권한 추가.

#### 🔴 Istio 설치 실패 (Connection Refused)
- **Issue**: Cloud Shell 세션 만료로 `kubectl` 컨텍스트 유실.
- **Solution**: `gcloud container clusters get-credentials ...` 로 재연결 후 설치.

#### 🔴 배포 후 Pod Pending
- **Issue**: 노드 리소스 부족.
- **Solution**: `kubectl describe pod` 확인. 현재 `e2-standard-2` 노드 2개(총 4 vCPU, 16GB)로 운영 중.

### 2. Version Control & Build

#### 🔴 Large File Push Error
- **Issue**: Terraform 바이너리 등 대용량 파일이 커밋됨.
- **Solution**: `.gitignore`에 `.terraform/` 추가 후 `git reset HEAD^`.

#### 🔴 Gradle Wrapper Execution
- **Issue**: 서브 모듈 디렉토리에서 `./gradlew` 실행 시 설정 누락.
- **Solution**: 루트 디렉토리에서 `./gradlew :auth-service:bootRun` 형식으로 실행 권장.

### 3. Application Runtime

#### 🔴 Missing Environment Variables
- **Issue**: `InjectionMetadata` 에러 발생.
- **Solution**: 환경변수(`JWT_SECRET` 등)를 실행 명령어에 포함하여 주입.

#### 🔴 Authorization 403 (Malformed Token)
- **Issue**: 헤더에 JSON 전체를 넣어서 인증 실패.
- **Solution**: `Bearer <Pure_Access_Token>` 형식으로 정확한 토큰 값만 전송.
