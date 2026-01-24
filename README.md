# MSA Project

Spring Boot 기반의 마이크로서비스 아키텍처(MSA) 프로젝트입니다.

## 프로젝트 구조

| 서비스 | 설명 | 포트 |
| --- | --- | --- |
| **[auth-service](./auth-service)** | 사용자 인증 및 JWT 토큰 발급/검증 | 8082 |
| **order-service** | 주문 처리 (개발 예정) | - |
| **payment-service** | 결제 처리 (개발 예정) | - |

## Auth Service
`auth-service`는 사용자의 로그인 요청을 받아 DB(`users` 테이블)에서 검증 후 JWT 토큰을 발급합니다.
자세한 내용은 [auth-service/README.md](./auth-service/README.md)를 참고하세요.

### 주요 기능
- **로그인**: `POST /auth/login`
- **토큰 검증**: `GET /auth/validate`
- **데이터 시딩**: 초기 실행 시 기본 유저(`user`/`password`) 자동 생성

## 실행 방법 (전체)
각 서비스 디렉토리의 `README.md`를 참조하여 개별적으로 실행할 수 있습니다.
공통적으로 `.env` 파일을 통해 환경 변수(DB 접속 정보 등)를 관리합니다.