# Auth Service

인증 및 권한 부여를 담당하는 마이크로서비스입니다. JWT(Json Web Token) 기반의 인증 방식을 사용하며, 사용자 정보를 관리합니다.

## 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.10
- **Database**: PostgreSQL (JPA/Hibernate)
- **Security**: JWT (HS256)
- **Monitoring**: Zipkin (Tracing)

## 실행 방법

### 로컬 실행
프로젝트 루트에서 다음 명령어를 실행합니다.

```bash
# 1. 환경 변수 설정 (.env 파일 필요)
source .env

# 2. 빌드 및 실행
./auth-service/gradlew -p auth-service build -x test && java -jar auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar
```

실행 후 `http://localhost:8082`에서 서비스가 동작합니다.

> **Note**: 초기 실행 시 `DataInitializer`가 테스트용 계정(`user` / `password`)을 자동으로 생성합니다.

## API 명세

### 1. 로그인 (Login)
사용자 자격 증명을 확인하고 JWT 토큰을 발급합니다.

- **URL**: `POST /auth/login`
- **Request Body**:
  ```json
  {
    "username": "user",
    "password": "password"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```

### 2. 토큰 검증 (Validate)
발급된 JWT 토큰의 유효성을 검증하고 사용자 ID를 반환합니다.

- **URL**: `GET /auth/validate`
- **Query Parameter**: `token` (JWT 토큰)
- **Response (200 OK)**:
  `Valid Token for user: {username}`
