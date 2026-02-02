# MSA Project (FISA-L2J) - Cloud Native Migration

<img width="2816" height="1286" alt="MSA" src="https://github.com/user-attachments/assets/488575a0-c6f2-4559-8a03-cb13c019d287" />

Spring Boot 기반의 마이크로서비스 아키텍처(MSA) 계좌·거래 데모 프로젝트입니다.  
기존 VM 기반 배포에서 **Kubernetes(GKE) 및 Istio Service Mesh** 환경으로 마이그레이션되었습니다.

인증(Auth), 계좌(Account), 거래(Transaction) 서비스로 구성되어 있으며, 입금/출금 도메인과 서비스 간 통신, 장애 격리, 분산 트레이싱 등 MSA의 핵심 패턴들을 구현했습니다.

## 📌 목차

* [1. 👤 팀원 소개](#team)
* [2. 💡 주제 선정 배경](#background)
* [3. 🏗 아키텍처 및 기술 스택](#architecture)
* [4. 🚀 Cloud Native 배포 가이드](#deploy)
* [5. 🌟 핵심 기능](#key-features)
* [6. 📚 API 명세서](#api)
* [7. 🚀 로컬 실행 방법](#local)
* [8. 🛠 트러블슈팅](#troubleshooting)

---

<h2 id="team">1. 👤 팀원 소개 (Team Members)</h2>

| <img src="https://github.com/chaeyuuu.png" width="150"> | <img src="https://github.com/woojinni.png" width="150"> | <img src="https://github.com/YongwanJoo.png" width="150"> |
| :---: | :---: | :---: |
| **이채유** | **장우진** | **주용완** |
| [@chaeyuuu](https://github.com/chaeyuuu) | [@woojinni](https://github.com/woojinni) | [@YongwanJoo](https://github.com/YongwanJoo) |



<br>

<h2 id="background">2. 💡 주제 선정 배경 (Project Background)</h2>

본 프로젝트는 **MSA와 Cloud Native 환경에 대한 학습**을 목표로 진행되었다.  
단일 애플리케이션에서 **서비스 분리·독립 배포·트래픽 제어**를 경험하는 과정에서,  
실제 GKE와 Istio 기반의 **운영 가능한 구조**를 구축하는 것이 목표이다.

> "VM 기반 배포에서 Kubernetes와 Service Mesh로 어떻게 전환할 수 있을까?"  
> 라는 질문에서 출발하여, **Auth / Account / Transaction** 도메인으로 분리된  
> 입금·출금 파이프라인을 **GKE + Istio** 환경에서 동작하도록 설계하였다.

<br>

<h2 id="architecture">3. 🏗 아키텍처 및 기술 스택</h2>

### Infrastructure (Cloud Native)
- **Cloud**: Google Cloud Platform (GKE Standard Cluster, Artifact Registry)
- **IaC**: Terraform (GKE Cluster & Node Pool 프로비저닝)
- **Service Mesh**: Istio (Traffic Management, Ingress Gateway)
- **CI/CD**: GitHub Actions (Docker Build -> Artifact Registry -> GKE Deploy)
- **RDBMS**: PostgreSQL (GKE 내 StatefulSet, Logical DB 분리: `auth_db`, `account_db`, `transaction_db`)
- **Cache**: Redis (Auth Service 토큰 관리)
- **Tracing**: Zipkin (분산 트레이싱)

### Microservices
| 서비스 | 기술 스택 | 주요 역할 | 포트 |
| --- | --- | --- | --- |
| **Auth Service** | Spring Security, JWT(RS256), Redis | 사용자 가입/로그인/로그아웃, JWKS 공개키 제공 (Port: 8082) | 8082 |
| **Account Service** | Spring Boot, OpenFeign, Resilience4j | 계좌/거래 요청, 입금·출금 API, 서킷 브레이커 | 8080 |
| **Transaction Service** | Spring Boot, JPA | 잔액·거래 처리(입금/출금 실행) | 8081 |

---

<h2 id="deploy">4. 🚀 Cloud Native 배포 가이드 (GKE & Istio)</h2>

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
# EXTERNAL-IP 확인 후: http://<EXTERNAL-IP>/account
```

**Kiali 대시보드 (Service Mesh 시각화)**:
```bash
istioctl dashboard kiali
```

**Grafana 대시보드 (매트릭 시각화)**:
```bash
# 1. 포트 포워딩
kubectl port-forward svc/grafana 3000:3000 -n istio-system

# 2. 접속: http://localhost:3000 (ID: admin / PW: admin)
# 3. 대시보드 추가: 'Import' 메뉴 -> ID '7639' (Istio Mesh Dashboard) 입력
```

---

<h2 id="key-features">5. 🌟 핵심 기능 (Key Features)</h2>



### 1. Istio Service Mesh
- **Traffic Management**: `Istio Gateway`를 통해 모든 외부 트래픽을 단일 진입점으로 관리합니다.
- **Sidecar Proxy**: 각 서비스 파드에 Envoy 프록시가 주입되어 트래픽을 가로채고 제어합니다.

### 2. Token Propagation (토큰 전파)
- **FeignClientInterceptor**를 통해 `Account Service`로 들어온 요청의 JWT 토큰을 추출하여, 내부적으로 호출하는 `Transaction Service`로 전달합니다.
- 이를 통해 마이크로서비스 간의 호출에서도 **사용자 인증 정보(User Context)가 끊기지 않고 유지**됩니다.

### 3. Resilience (회복 탄력성)
- **Circuit Breaker**: `Transaction Service` 장애 시 `Account Service`의 **Resilience4j**가 동작하여 장애 전파를 차단합니다. Account Service는 Fallback 응답을 반환하여 시스템 전체 중단을 방지합니다.

### 4. Database Isolation
- 단일 PostgreSQL 파드 내에서 `auth_db`, `account_db`, `transaction_db`로 논리적 분리를 구현했습니다. (Database-per-service 패턴 준수)
- `k8s/secret.yaml`을 통해 DB 자격증명을 안전하게 관리합니다.

---

<h2 id="api">6. 📚 API 명세서 (API Documentation)</h2>

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

### 2. Account Service (Port: 8080)
계좌 거래 요청 - 입금/출금 (Requires JWT Authentication)

> **Note**: 모든 요청의 Header에 `Authorization: Bearer <Token>`이 필요합니다.
> 클라이언트는 **amount만** 전송하며, **userId는 서버에서 JWT로 추출**하여 Transaction Service 요청에 포함합니다.

#### 입금 (Deposit)
- **URL**: `POST /account/deposit`
- **Request**:
  ```json
  {
    "amount": 10000
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "transactionId": 1,
    "transactionId": 1,
    "userId": "testuser",
    "amount": 10000,
    "newBalance": 10000,
    "status": "SUCCESS",
    "createdAt": "..."
  }
  ```
- **Circuit Open 시 (Transaction Service 장애)**: Resilience4j Fallback 응답 예시
  ```json
  {
    "userId": 1,
    "amount": 10000,
    "newBalance": 0,
    "status": "FAILED"
  }
  ```

#### 출금 (Withdrawal)
- **URL**: `POST /account/withdrawal`
- **Request**:
  ```json
  {
    "amount": 5000
  }
  ```
- **Response**: `201 Created`
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
- **Circuit Open 시 (Transaction Service 장애)**: Fallback 응답 예시 — `status: "FAILED"`, `newBalance: 0`
- **Note**: 잔액 부족 시 `400 Bad Request` (Transaction Service에서 처리). 에러 본문 예: `"Insufficient balance. Current: 1000, Requested: 5000"`

---

### 3. Transaction Service (Port: 8081)
잔액·거래 처리 (일반적으로 Account Service에서 내부 호출). Account Service가 **TransactionProcessRequest**(userId, amount)를 가공하여 전달합니다.

#### 입금 처리 (내부)
- **URL**: `POST /transaction/deposit`
- **Request** (Account Service가 JWT에서 추출한 userId와 클라이언트 amount를 조합하여 전송):
  ```json
  {
    "userId": "testuser",
    "amount": 10000
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "transactionId": 1,
    "newBalance": 10000,
    "status": "SUCCESS",
    "createdAt": "..."
  }
  ```

#### 출금 처리 (내부)
- **URL**: `POST /transaction/withdrawal`
- **Request**:
  ```json
  {
    "userId": 1,
    "amount": 5000
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "transactionId": 2,
    "newBalance": 5000,
    "status": "SUCCESS",
    "createdAt": "..."
  }
  ```
- **잔액 부족 시**: `400 Bad Request`, 본문 예시: `"Insufficient balance. Current: 1000, Requested: 5000"` (GlobalExceptionHandler가 InsufficientBalanceException 메시지를 그대로 반환)

---

<h2 id="local">7. 🚀 로컬 실행 방법 (Local Development)</h2>

### 1. 자동 설정 스크립트 사용 (권장)
프로젝트 루트에서 제공되는 `guide_setup.sh` 스크립트를 사용하면, `.env.local` 파일 생성과 로컬 키 발급, Docker 인프라 실행을 한 번에 처리해줍니다.

```bash
# 실행 권한 부여
chmod +x scripts/guide_setup.sh

# 스크립트 실행 (DB 비밀번호 설정 가능)
DB_PASSWORD=your_secure_password ./scripts/guide_setup.sh
```

### 2. 서비스 실행
인프라(DB, Redis 등)가 준비되면, 생성된 `.env.local`을 로드하여 각 서비스를 실행합니다. 터미널 3개를 열어 각각 실행하세요.

#### Auth Service (Terminal 1)
```bash
source .env.local && ./gradlew :auth-service:bootRun
```

#### Account Service (Terminal 2)
```bash
source .env.local && ./gradlew :account-service:bootRun
```

#### Transaction Service (Terminal 3)
```bash
source .env.local && ./gradlew :transaction-service:bootRun
```
*모든 서비스를 띄워야 전체 흐름(로그인 → 입금/출금) 테스트가 가능합니다.*

---

<h2 id="troubleshooting">8. 🛠 트러블슈팅 (Troubleshooting)</h2>

### 1. Cloud & Infrastructure (GKE, Terraform)

#### 🔴 GCP IAM Permission Denied (403)
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

#### 🔴 Docker Sudo Authentication
- **Issue**: `sudo docker pull` 실행 시 권한 에러 발생 (credential helper가 root에 적용 안 됨).
- **Cause**: GCP `gcloud auth configure-docker`는 현재 유저에게만 적용됨.
- **Solution**: `deploy.yml`에서 Access Token을 직접 추출하여 로그인하는 방식으로 변경.
  ```yaml
  gcloud auth print-access-token | sudo docker login -u oauth2accesstoken --password-stdin https://asia-northeast3-docker.pkg.dev
  ```

### 4. Application Verification (Runtime & Logic)

#### 🔴 Build Configuration - Redundant Plugin
- **Issue**: `Account Service` 등 서비스 실행 시 빌드 실패.
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
- **Issue 2**: 입금/출금 요청 시 `403 Forbidden`.
  - **Cause**: `Authorization` 헤더에 JWT 토큰 문자열만 넣어야 하는데, JSON 응답 전체(`{"accessToken":...}`)를 넣음.
  - **Solution**: `curl` 및 `python` 파싱을 통해 `accessToken` 값만 정확히 추출하여 헤더에 주입.

### 5. Service Communication (NATS & Istio)

#### 🔴 NATS Connection Timeout
- **Issue**: `account-service` 기동 시 `TimeoutException` 발생하며 CrashLoopBackOff.
- **Cause**: GKE 환경에서 NATS 연결 초기화 지연(네트워크 레이턴시). 기본 타임아웃(2초) 부족.
- **Solution**: `Options.builder().connectionTimeout(Duration.ofSeconds(10))`로 타임아웃 10초로 증가.

#### 🔴 Istio Protocol Detection (NATS)
- **Issue**: NATS 연결은 되지만 메시지 발행/구독 불가 또는 연결 끊김.
- **Cause**: Istio가 Service Port 이름(`client`)만 보고 HTTP로 오판단하여 프로토콜 스니핑 오류 발생.
- **Solution**: Service Port 이름을 `tcp-client`로 변경하여 TCP 프로토콜임을 명시 (`appProtocol` 또는 접두사 사용).

### 6. Business Logic (SAGA)

#### 🔴 Balance Not Updating
- **Issue**: 트랜잭션이 `SUCCESS`로 완료되었으나 잔액이 0원으로 유지됨.
- **Cause**: `TransactionResultSubscriber`가 트랜잭션 성공 이벤트를 수신하고 상태(`TransactionRecord`)만 업데이트하고, 실제 계좌 잔액(`Account`) 업데이트 로직이 누락됨.
- **Solution**: 이벤트 수신 시 `AccountRepository`를 통해 잔액을 업데이트하는 로직 추가 (`newBalance` 반영).

#### 🔴 Account Details Not Accessible
- **Issue**: 잔액 확인을 위해 `GET /account` 호출 시 `405 Method Not Allowed`.
- **Cause**: `AccountController`에 조회용 GET 엔드포인트가 구현되지 않음.
- **Solution**: `GET /account` 엔드포인트 및 서비스 로직 추가.
