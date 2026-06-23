# 모니: 모의 머니 (moni) — Claude Code 가이드

이 문서는 Claude Code가 이 저장소에서 작업할 때 항상 참고해야 하는 최상위 컨텍스트입니다.
세부 자료(기획, API 명세, ERD, 인프라 설계, MSA 가이드라인, AI 검증 로그)는 `docs/.context/` 디렉토리에
나누어 정리되어 있으니, 작업 범위에 맞는 문서를 **필요할 때 직접 열어서** 참고하세요.
(이 파일에는 전부 합치지 않고 링크만 둡니다 — 컨텍스트 절약 목적)

---

## 1. 프로젝트 한 줄 설명

초보자를 위해 복잡한 금융 지표·뉴스를 AI로 알기 쉽게 제공하고, 개인 맞춤형 포트폴리오 분석과
실시간 주식 거래를 지원하는 **모의 투자 플랫폼**. Spring Boot 3.5 / Java 21 기반 MSA로 구현 중.

자세한 배경·목표·기능 우선순위(P0/P1/P2)는 → `docs/.context/01-PROJECT-OVERVIEW.md`

---

## 2. 현재 저장소 상태 (중요)

- 모든 서비스는 **스캐폴드(빈 Application 클래스 + application.yml)만 존재**하는 초기 상태입니다.
- 실제 도메인 로직(엔티티, 컨트롤러, 서비스, 레포지토리 등)은 아직 구현되지 않았습니다.
- 코드를 생성할 때는 아래 "5. 코드 컨벤션·아키텍처 규칙"을 반드시 따르세요.
- `build/` 디렉토리는 Gradle 빌드 산출물입니다. **절대 직접 수정하지 마세요** (git에도 커밋 금지 대상).

---

## 3. 서비스 구성 (포트/책임/담당자)

| 서비스                  | 패키지                     | 포트    | 책임                                                                      | 기획상 담당자 |
|----------------------|-------------------------|-------|-------------------------------------------------------------------------|---------|
| config-server        | `com.moni.config`       | 8888  | 중앙 설정 서버                                                                | 동원      |
| eureka-server        | `com.moni.eureka`       | 8761  | 서비스 디스커버리                                                               | -       |
| api-gateway          | `com.moni.gateway`      | 8080  | API 게이트웨이(Spring Cloud Gateway), 외부 단일 진입점                              | 동원      |
| user-service         | `com.moni.user`         | 19090 | 회원가입/로그인/인증(JWT, OAuth), 투자 성향·관심사·관심종목                                 | 동원      |
| trade-service        | `com.moni.trade`        | 19091 | 모의 매수/매도, 거래 내역, 모의 계좌(balance)                                         | 동민      |
| stock-service        | `com.moni.stock`        | 19092 | 실시간 시세/종목 정보, 테마, 인기 종목                                                 | 영욱      |
| portfolio-service    | `com.moni.portfolio`    | 19093 | 포트폴리오 대시보드, 수익률 계산, AI 포트폴리오 분석 연동                                      | 설아      |
| notification-service | `com.moni.notification` | 19094 | 사용자 맞춤 알림                                                               | 혜수      |
| payment-service      | `com.moni.payment`      | 19095 | AI 분석 구독/결제(아임포트)                                                       | 혜수      |
| ai-service           | `com.moni.ai`           | 19096 | RAG 기반 기업 이슈 분석, 뉴스 요약/수집, 포트폴리오 AI 분석                                  | 지은      |
| admin-service        | `com.moni.admin`        | 19097 | 관리자 웹 UI (Thymeleaf SSR), 유저 조회/정지/삭제. api-gateway 우회, Okta OIDC(세션 인증) | 동원      |

> ⚠️ trade-service와 portfolio-service 모두 `account`/`holding` 개념을 갖고 있습니다.
> ERD 상 trade-service가 "모의투자 계좌(현금/보유종목, 거래 처리)"의 **소스 오브 트루스**이고,
> portfolio-service의 `account`/`holding`은 대시보드 계산/조회용으로 보입니다.
> 새 코드를 작성하기 전에 `docs/.context/03-ERD.md`의 5번(Portfolio-Service)과 6번(Trade-Service) 섹션을
> 비교해서 중복/소유권 문제를 사용자에게 먼저 확인하세요. **데이터 소유권이 불명확하면 추측해서
> 구현하지 말고 질문하세요.**

인프라 포트(DB/Redis/Kafka 등)는 → `docs/.context/04-INFRA-ARCHITECTURE.md`

---

## 4. 기술 스택 / 빌드 명령어

- Java 21, Spring Boot 3.5.0, Spring Cloud 2025.0.0, Gradle (멀티모듈)
- 통신: 동기 = OpenFeign(Rest), 비동기 = Kafka
- DB: PostgreSQL (서비스별 DB 분리, `docker-compose.infra.yml` 참고)
- 캐시: Redis
- 인증: OAuth + JWT
- 분산 추적: Zipkin / 모니터링: Prometheus + Grafana
- 문서화: Swagger(springdoc-openapi) / 테스트: JUnit5, Postman
- Lint: Checkstyle (`config/checkstyle/checkstyle.xml`, `maxWarnings = 0`)

### 자주 쓰는 명령어

```bash
# 서비스 애플리케이션 전체 기동 (server)
docker compose -f docker-compose.yml up -d --build

# 인프라(DB, Redis, Kafka 등) 기동
docker compose -f docker-compose.infra.yml up -d

# 모니터링(Prometheus/Grafana) 기동
docker compose -f docker-compose.monitor.yml up -d

# Gradle 빌드 (도커 이미지 생성 전 권장)
./gradlew clean build -x test

# 전체 빌드 (테스트 포함)
./gradlew build

# 특정 서비스만 빌드/테스트
./gradlew :user-service:build
./gradlew :user-service:test

# Checkstyle만 검사
./gradlew :user-service:checkstyleMain

# 특정 서비스 실행 (로컬, config-server/eureka-server 먼저 기동 필요)
./gradlew :user-service:bootRun
```

부팅 순서: `config-server` → `eureka-server` → `api-gateway` → 나머지 도메인 서비스. ( 도커 서비스 실행시 헬스체크 자동 실행 )

---

## 5. 코드 컨벤션 / 아키텍처 규칙 (필수)

본 프로젝트는 **계층형 아키텍처(4계층)** 가 필수이며, 여력이 되면 DDD 개념(애그리거트, 도메인 이벤트)을
선택적으로 적용합니다. 새 서비스 코드를 작성/수정할 때 아래 패키지 구조와 책임 분리를 따르세요.

```
com.moni.<service>
├── presentation        # Controller, Request/Response DTO, 예외 핸들러
│   ├── controller
│   └── dto
├── application          # Use-case 서비스(트랜잭션 경계), 오케스트레이션
│   └── service
├── domain                # Entity/VO, 도메인 서비스, 비즈니스 규칙
│   ├── entity (또는 model)
│   └── service (도메인 서비스, optional)
└── infrastructure        # Repository 구현, FeignClient, Kafka Producer/Consumer, Redis 접근
    ├── repository
    ├── client (Feign)
    └── messaging (Kafka)
```

규칙:
1. **비즈니스 로직은 domain 계층에 응집.** application 서비스(Fat Service 금지)는 오케스트레이션만.
2. JPA Entity는 도메인 엔티티에 직접 어노테이션을 붙이는 방식 허용(AI 검증 로그에서 합의된 사항 — 순수 도메인 모델 분리는 과도한 엔지니어링으로 판단).
3. 이벤트 발행은 `ApplicationEvent` → 인프라 리스너 → `KafkaTemplate` 구조 권장(메시징 교체 시 도메인/응용 코드 영향 없도록).
4. 서비스 간 동기 호출(FeignClient)에는 **반드시 timeout을 명시**하고, 가능하면 Resilience4j Circuit Breaker + Fallback을 적용.
5. "사용자 응답에 꼭 필요한 것만 동기(Feign), 나머지는 전부 비동기(Kafka)" 원칙을 지킨다.
6. 트랜잭션 경계 = 서비스 경계. 서비스를 걸치는 작업은 `이벤트 발행 → 후처리(최종적 일관성)` 패턴으로.
7. 모든 컨트롤러는 `/api/v1/...` 프리픽스를 사용한다 (→ `docs/.context/02-API-SPEC.md` 의 URL 그대로 구현).
8. 공통 감사 필드(`created_at/by`, `updated_at/by`, `deleted_at/by`)는 `common` 모듈에 BaseEntity로 추출해서 재사용 (`docs/03-ERD.md` 0번 항목 참고).
9. Checkstyle 경고 0개를 유지해야 빌드가 통과한다(`maxWarnings = 0`).
10. 신규 코드 작성 시 단위 테스트 커버리지 80% 이상을 목표로 한다(CI 게이트).

상세 서비스 분리/통신/안티패턴 기준 → `docs/05-MSA-GUIDELINES.md`

---

## 6. CI/CD 흐름 (참고)

- `develop` 브랜치 PR: Lint Check → Build → Unit Test & Coverage(80% 미달 시 차단) → 수동 머지
- `main` 브랜치 PR: Lint Check → Build → Containerization(ECR) → Unit Test & Coverage → Integration Test(docker-compose) → 실패 시 이전 tag로 rollback

자세한 인프라/배포/모니터링 설계는 → `docs/04-INFRA-ARCHITECTURE.md`

---

## 7. AI 활용 원칙 (이 프로젝트의 핵심 제출물)

이 프로젝트는 "AI가 설계를 대신 해주는 것"이 아니라 **사용자가 설계하고 AI가 검증**하는 것이
핵심 산출물입니다. Claude가 아키텍처/설계 관련 제안을 할 때는:

- 제안과 함께 **트레이드오프**를 명시하고, 무조건 동의/추천만 하지 말 것.
- 사용자가 결정을 내리면 `docs/06-AI-VALIDATION-LOG.md`에 검증 항목/AI 피드백 요약/수용 여부/판단 근거
  형식으로 기록을 도와줄 것 (이 로그 자체가 면접용 포트폴리오 산출물).
- 이미 "거부"로 기록된 결정(예: k8s 미사용, 사용자/인증 서비스 미분리, 재고 애그리거트 미분리 등)은
  재차 권유하지 말고 기존 결정을 존중한다. → 기존 로그는 `docs/06-AI-VALIDATION-LOG.md` 참고.

---

## 8. docs/ 디렉토리 안내

| 파일                                       | 내용                                     | 언제 참고                         |
|------------------------------------------|----------------------------------------|-------------------------------|
| `docs/.context/01-PROJECT-OVERVIEW.md`   | 주제/목표/기능 정의/우선순위(P0~P2)/담당자            | 새 기능 작업 전, 우선순위 확인            |
| `docs/.context/02-API-SPEC.md`           | 서비스별 전체 API 명세(URL/Method)             | 컨트롤러/DTO 작성 시                 |
| `docs/.context/03-ERD.md`                | 서비스별 테이블 스키마(ERD)                      | Entity/Repository/마이그레이션 작성 시 |
| `docs/.context/04-INFRA-ARCHITECTURE.md` | 인프라 구성도, 비기능 요구사항, CI/CD, 모니터링         | 인프라/Docker/k8s/모니터링 작업 시      |
| `docs/.context/05-MSA-GUIDELINES.md`     | MSA 서비스 분리 기준, 통신 방식 선택 기준, 안티패턴 체크리스트 | 서비스 경계/통신 방식을 결정할 때           |
| `docs/.context/06-AI-VALIDATION-LOG.md`  | 기존 AI 설계 검증 로그 + 새 로그 작성 템플릿           | 설계 의사결정을 검증/기록할 때             |
