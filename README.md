<!-- 상단 배너 이미지 교체 예정 -->
<div align="center">
  <img src="docs/images/banner1.png" alt="moni banner-1" width="1920"/>
  <img src="docs/images/banner2.png" alt="moni banner-2" width="1920"/>

  <h1>moni · 모니</h1>

![개발기간](https://img.shields.io/badge/개발_기간-2026.06_~_2026.07.13-0057FF?style=flat&labelColor=111111)

  <p>MSA 기반 복잡한 금융 지표와 뉴스를 AI가 쉽게 풀어주는 초보자 친화적 모의 투자 플랫폼

<hr/>

<p align="center">
  <a href="#프로젝트-소개">소개</a> &nbsp;&bull;&nbsp;
  <a href="#프로젝트-목표">목표</a> &nbsp;&bull;&nbsp;
  <a href="#배포-주소">배포</a> &nbsp;&bull;&nbsp;
  <a href="#팀원-소개">팀원</a> &nbsp;&bull;&nbsp;
  <a href="#기술-스택">기술 스택</a> &nbsp;&bull;&nbsp;
  <a href="#마이크로서비스-구성">서비스 구성</a> &nbsp;&bull;&nbsp;
  <a href="#패키지-구조">패키지 구조</a> &nbsp;&bull;&nbsp;
  <a href="#도메인-정의">도메인 정의</a> &nbsp;&bull;&nbsp;
  <a href="#핵심-비즈니스-로직">비즈니스 로직</a> &nbsp;&bull;&nbsp;
  <a href="#erd-명세서">ERD</a> &nbsp;&bull;&nbsp;
  <a href="#api-명세서">API</a> &nbsp;&bull;&nbsp;
  <a href="#인프라-아키텍처">인프라</a> &nbsp;&bull;&nbsp;
  <a href="#cicd">CI/CD</a> &nbsp;&bull;&nbsp;
  <a href="#실행-방법">실행</a>
</p>

</div>

## 프로젝트 소개

***

초보 투자자를 위해 복잡한 금융 정보를 AI로 단순화하고, 실제 자금 없이 모의 매수·매도를 경험할 수 있는 **모의 투자 플랫폼**입니다.

- Spring Cloud(Eureka, Gateway, OpenFeign) 기반 멀티모듈 MSA
- OAuth2 소셜 로그인(Google, Kakao) + JWT 인증/인가
- Kafka 기반 비동기 이벤트 처리로 서비스 간 결합도 최소화
- Redis Cluster를 활용한 실시간 시세 캐싱
- pgvector + RAG 기반 기업 뉴스 분석 및 포트폴리오 AI 리포트 (ai-service)
- Okta OIDC 세션 인증 기반 관리자 웹 UI (admin-service, Thymeleaf SSR)
- Grafana Alloy · Loki · Tempo · Prometheus · Alertmanager · Grafana 기반 풀스택 모니터링 (로그 · 트레이싱 · 메트릭 · 알림)
- 모든 엔티티 Soft Delete 처리 (`deleted_at`, `deleted_by`)

 <br>

### 관련 저장소

| 저장소                                                                           | 설명                                                          |
|:------------------------------------------------------------------------------|:------------------------------------------------------------|
| [moni-web](https://github.com/NaejusikSamjo/moni-web)                         | Next.js 16 기반 웹 클라이언트                                       |
| [moni-monitor](https://github.com/NaejusikSamjo/moni-monitor)                 | Grafana Alloy · Loki · Tempo · Prometheus · Grafana 모니터링 구성 |
| [moni-infra-terraform](https://github.com/NaejusikSamjo/moni-infra-terraform) | AWS 인프라 Terraform 코드                                        |

 <br>

## 프로젝트 목표

***

> **비즈니스 목표**
>
- AI 기반 기업 이슈 및 뉴스 요약을 제공하여 초보 투자자의 정보 접근성을 높인다.
- 실시간 시세 기반 모의투자를 통해 실제 투자와 유사한 경험을 제공한다.
- 개인 맞춤형 포트폴리오 분석으로 투자 성향에 맞는 의사결정을 지원한다.

> **기술적 목표**
>
**1.  모니터링, 인프라, 서비스 등 언제든 스케일 아웃 가능한 구조 구축**
- 멀티 모듈 구조를 넘어 완전한 MSA 구조로 필요시 단일 노드로 스케일 아웃 가능하게 한다.
- 서비스뿐 아니라 모니터링, 인프라 등을 독립 레포지토리로 분리, 코드로 관리 및 **서비스 간 결합도 최소를 목표**로 한다.

**2. 외부 API 장애 격리를 위한 Fallback 시스템 구축**
- 외부 연동 서비스(금융, 뉴스, AI, 결제 등)가 필수적인 환경에서, 특정/외부 API의 장애나 지연이 전체 시스템에 전파되지 않도록 서킷 브레이커 및 Fallback 구조, Retry 등을 구축해 **높은 가용성 목표**로 한다.

**3. 트랜잭션을 위한 정합성 보장 및 동시성 제어**
- 모의 투자 및 서비스 결제와 같은 로직에서 멱등성을 보장한다.
- **상황에 맞추어 낙관적/비관적 락 그리고 Redisson 분산락 등을 전략적으로 도입**해 동시성 문제 제어 목표로 한다.

**4. RAG 및 LLM 분석**
- 필터링으로 불필요한 뉴스 기사를 제거해 RAG 품질을 향상한다.
- 뉴스 수집, RAG, LLM 분석까지 **자동화된 파이프라인을 구축**하여 기업, 시장 이슈를 구조화된 형식 제공 목표로 한다.


 <br>

## 배포 주소

***

| 환경          | URL                                                |
|:------------|:---------------------------------------------------|
| 웹 클라이언트     | [https://www.moni.my](https://www.moni.my)         |
| API Gateway | [https://api.moni.my](https://api.moni.my)         |
| Admin       | [https://admin.moni.my](https://admin.moni.my)     |
| Grafana     | [https://grafana.moni.my](https://grafana.moni.my) |

 <br>

## 팀원 소개

***

|                    **혜수 (Leader)**                     |                    **영욱 (Sub-Leader)**                    |                         **설아**                         |                          **동민**                           |                         **지은**                          |                          **동원**                           |
|:------------------------------------------------------:|:---------------------------------------------------------:|:------------------------------------------------------:|:---------------------------------------------------------:|:-------------------------------------------------------:|:---------------------------------------------------------:|
| <img src="https://github.com/hyesuhan.png" width="80"> | <img src="https://github.com/kimyounguk1.png" width="80"> | <img src="https://github.com/seola12e.png" width="80"> | <img src="https://github.com/DONGMIN-777.png" width="80"> | <img src="https://github.com/Jieunbakk.png" width="80"> | <img src="https://github.com/won2dev-lab.png" width="80"> |
|        [@hyesuhan](https://github.com/hyesuhan)        |      [@kimyounguk1](https://github.com/kimyounguk1)       |        [@seola12e](https://github.com/seola12e)        |      [@DONGMIN-777](https://github.com/DONGMIN-777)       |       [@Jieunbakk](https://github.com/Jieunbakk)        |      [@won2dev-lab](https://github.com/won2dev-lab)       |
|        Notification Service<br>Payment Service         |                       Stock Service                       |                   Portfolio Service                    |                       Trade Service                       |                       AI Service                        |               User Service<br>Admin Service               |

 <br>

## 기술 스택

***

| 분류           | 기술                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|:-------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Backend      | ![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=flat&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white) ![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-6DB33F?style=flat&logo=spring&logoColor=white) ![Netflix Eureka](https://img.shields.io/badge/Netflix_Eureka-6DB33F?style=flat&logo=spring&logoColor=white) ![OpenFeign](https://img.shields.io/badge/OpenFeign-6DB33F?style=flat&logo=spring&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat&logo=spring&logoColor=white) ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat&logo=thymeleaf&logoColor=white) ![Spring AI](https://img.shields.io/badge/Spring_AI-6DB33F?style=flat&logo=spring&logoColor=white) ![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=flat&logoColor=white) |
| Auth         | ![OAuth2](https://img.shields.io/badge/OAuth2-4285F4?style=flat&logo=google&logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=jsonwebtokens&logoColor=white) ![Okta](https://img.shields.io/badge/Okta_OIDC-007DC1?style=flat&logo=okta&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Database     | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white) ![pgvector](https://img.shields.io/badge/pgvector-4169E1?style=flat&logo=postgresql&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-FF4438?style=flat&logo=redis&logoColor=white) ![Redis Cluster](https://img.shields.io/badge/Redis_Cluster-FF4438?style=flat&logo=redis&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Messaging    | ![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat&logo=apachekafka&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Infra        | ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white) ![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=flat&logo=amazonaws&logoColor=white) ![AWS ECR](https://img.shields.io/badge/AWS_ECR-FF9900?style=flat&logo=amazonaws&logoColor=white) ![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=flat&logo=amazons3&logoColor=white) ![CloudFront](https://img.shields.io/badge/CloudFront-FF9900?style=flat&logo=amazonaws&logoColor=white) ![ALB](https://img.shields.io/badge/AWS_ALB-FF9900?style=flat&logo=amazonaws&logoColor=white) ![ACM](https://img.shields.io/badge/AWS_ACM-FF9900?style=flat&logo=amazonaws&logoColor=white) ![Terraform](https://img.shields.io/badge/Terraform-7B42BC?style=flat&logo=terraform&logoColor=white)                                                                                                                                                                                                                                                                                  |
| Monitoring   | ![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-000000?style=flat&logo=opentelemetry&logoColor=white) ![Grafana Alloy](https://img.shields.io/badge/Grafana_Alloy-F46800?style=flat&logo=grafana&logoColor=white) ![Loki](https://img.shields.io/badge/Grafana_Loki-F46800?style=flat&logo=grafana&logoColor=white) ![Tempo](https://img.shields.io/badge/Grafana_Tempo-F46800?style=flat&logo=grafana&logoColor=white) ![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=flat&logo=prometheus&logoColor=white) ![Grafana](https://img.shields.io/badge/Grafana-F46800?style=flat&logo=grafana&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| External API | ![KIS](https://img.shields.io/badge/KIS_한국투자증권-003087?style=flat&logoColor=white) ![Toss Payments](https://img.shields.io/badge/Toss_Payments-0064FF?style=flat&logo=tosspayments&logoColor=white) ![Naver](https://img.shields.io/badge/Naver_Search-03C75A?style=flat&logo=naver&logoColor=white) ![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=flat&logo=openai&logoColor=white) ![Gemini](https://img.shields.io/badge/Google_Gemini-4285F4?style=flat&logo=googlegemini&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Tools        | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black) ![Checkstyle](https://img.shields.io/badge/Checkstyle-0A0A0A?style=flat&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| CI/CD        | ![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |

 <br>

## 마이크로서비스 구성

***

| 서비스                  |  포트   | 핵심 기능                                                             |
|:---------------------|:-----:|:------------------------------------------------------------------|
| config-server        | 8888  | 중앙 설정 관리                                                          |
| eureka-server        | 8761  | 서비스 디스커버리                                                         |
| api-gateway          | 8080  | 라우팅, JWT 인증/인가, 외부 단일 진입점                                         |
| user-service         | 19090 | 회원가입/로그인, OAuth2, JWT 발급, 투자 성향·관심사·관심종목                          |
| trade-service        | 19091 | 모의 매수/매도, 거래 체결, 모의 계좌(balance) 관리, 수익률 계산                        |
| stock-service        | 19092 | 실시간 시세 (한국투자증권 KIS API), 종목 정보, 테마, 인기 종목 (Redis Cluster)         |
| portfolio-service    | 19093 | AI 분석 연동, 포트폴리오 조회                                                |
| notification-service | 19094 | 맞춤 알림 발송                                                          |
| payment-service      | 19095 | AI 분석 구독/결제 (토스)                                                  |
| ai-service           | 19096 | RAG 기반 기업 이슈 분석, 뉴스 요약/수집 (Naver), 포트폴리오 AI 리포트 (OpenAI + Gemini) |
| admin-service        | 19097 | 관리자 웹 UI (Thymeleaf SSR), Okta OIDC, 유저 조회/정지/삭제                  |

> 마이크로서비스 포트(19090~19097)는 내부 네트워크 전용이며 외부에 직접 노출되지 않습니다.
> 외부 요청은 API Gateway(8080)를 통해서만 접근 가능합니다.
> admin-service는 API Gateway를 우회하며 Okta OIDC 세션으로 독립 인증합니다.

 <br>

### 인프라 구성

| 인프라             |     포트      | 설명                               |
|:----------------|:-----------:|:---------------------------------|
| user-db         |    25432    | 회원 DB (PostgreSQL)               |
| portfolio-db    |    25433    | 포트폴리오 DB (PostgreSQL)            |
| stock-db        |    25434    | 시세 DB (PostgreSQL)               |
| notification-db |    25435    | 알림 DB (PostgreSQL)               |
| trade-db        |    25436    | 거래 DB (PostgreSQL)               |
| payment-db      |    25437    | 결제 DB (PostgreSQL)               |
| ai-db           |    25438    | AI DB (PostgreSQL + pgvector)    |
| Redis Cluster   | 27000~27002 | 시세 캐시 (stock-service 전용, 3-node) |
| Redis           |    26379    | 세션/캐시 (그 외 서비스)                  |
| Zookeeper       |    22181    | Kafka 코디네이션                      |
| Kafka           |    29092    | 메시지 브로커                          |

 <br>

## 패키지 구조

***


<details>
<summary>패키지 구조 보기</summary>

각 마이크로서비스는 기본적으로 **4계층 레이어드 아키텍처**를 따릅니다. DDD 개념(애그리거트, 도메인 이벤트)은 여력에 따라 선택적으로 적용합니다.

```
com.moni.<service>
├── global                # 전역 설정 — 공통 예외, 응답 포맷, 보안 설정, 공통 유틸
├── presentation          # 표현 계층 — Controller, Request/Response DTO, 예외 핸들러
│   ├── controller
│   └── dto
├── application           # 응용 계층 — Use-case 서비스, 트랜잭션 경계, 오케스트레이션
│   └── service
├── domain                # 도메인 계층 — Entity/VO, 비즈니스 규칙, 도메인 서비스
│   ├── entity (또는 model)
│   └── service
└── infrastructure        # 인프라 계층 — Repository 구현, FeignClient, Kafka, Redis
    ├── repository
    ├── client            # OpenFeign
    └── messaging         # Kafka Producer/Consumer
```

> **stock-service** 는 실시간 시세 처리의 특성상 **헥사고날 아키텍처(Ports & Adapters)** 를 적용합니다.
> 외부 어댑터(KIS API, WebSocket, Redis)를 포트로 추상화해 도메인 코드가 외부 의존성에 영향받지 않도록 분리합니다.

서비스 간 이벤트 발행은 `ApplicationEvent` → 인프라 리스너 → `KafkaTemplate` 구조를 권장합니다.
(메시징 교체 시 도메인·응용 계층 코드에 영향이 없도록 분리합니다.)

**common 모듈 설계 원칙:** `common`은 각 서비스가 직접 빈을 주입해서 사용하는 구조로, 공유 라이브러리 의존성 방식을 피했습니다. 처음부터 멀티모듈 분리 및 공통 라이브러리화가 즉시 가능한 구조로 설계되어 있습니다.

서비스별 도메인 구성:

```
user-service
├── auth/       # 로그인, JWT 발급, OAuth2 (Google/Kakao)
├── user/       # 사용자 엔티티, 투자 성향, 관심사, 프로필
└── admin/      # 관리자 전용 — 유저 조회/정지/삭제

trade-service
├── asset/      # 조회·계산용 계좌 스냅샷, 수익률 계산
├── account/    # 모의 계좌 (현금 잔고, 보유 종목) — 소스 오브 트루스
├── trade/      # 매수/매도 주문 처리, 체결
└── holding/    # 보유 종목 내역

stock-service
├── stock/      # 종목 마스터, 실시간 시세, 인기 종목
└── theme/      # 테마별 종목 분류

portfolio-service
└── portfolio/  # 포트폴리오 AI 분석, 포트폴리오 조회

notification-service
└── notification/  # 알림 설정, 발송 이력

payment-service
└── payment/    # 구독 플랜, 결제 처리 (토스 연동)

ai-service
├── news/       # 뉴스 수집, 요약 (RAG)
├── analysis/   # 기업 이슈 분석
└── report/     # 포트폴리오 AI 리포트 생성

admin-service
└── admin/      # 관리자 UI (Thymeleaf), 유저 목록/상세/제재 처리
```

</details>

 <br>

## 도메인 정의

***

| 도메인          | 설명                                                |
|:-------------|:--------------------------------------------------|
| User         | 회원가입/로그인, OAuth2 소셜 인증, 투자 성향 측정, 관심 섹터·관심 종목 설정  |
| Trade        | 모의 매수/매도 체결, 모의 계좌(현금 잔고·보유 종목) 관리, 자산 현황, 수익률 계산 |
| Stock        | 실시간 시세, 종목 마스터 데이터, 테마, 인기 종목, Redis Cluster 캐싱   |
| Portfolio    | AI 리포트 연동, 포트폴리오 조회                               |
| Notification | 가격 알림·공지 발송, 알림 수신 이력 관리                          |
| Payment      | AI 분석 기능 구독 플랜, 결제(아임포트 연동)                       |
| AI           | RAG 기반 기업 이슈·뉴스 분석, 포트폴리오 맞춤 리포트 생성               |
| Admin        | 관리자 전용 UI — 유저 목록 조회, 계정 정지/삭제, 뉴스 등록             |

 <br>

### 상태 흐름

***

**사용자 계정**
```
ACTIVE(정상)
  → SUSPENDED(정지)   # 관리자 제재
  → DELETED(탈퇴/삭제)
```

**모의 거래 주문**
```
PENDING(주문 대기)
  → COMPLETED(체결 완료)
  → CANCELLED(주문 취소)
```

**결제/구독**
```
PENDING(결제 대기)
  → ACTIVE(구독 활성)
  → EXPIRED(구독 만료)
  → CANCELLED(구독 취소)
```

### 서비스 간 통신 원칙

> **사용자 응답에 꼭 필요한 것만 동기(OpenFeign), 나머지는 전부 비동기(Kafka)**

| 통신 방식          | 사용 사례                            |
|:---------------|:---------------------------------|
| OpenFeign (동기) | 매수 시 시세 조회, 포트폴리오 요약용 계좌 조회      |
| Kafka (비동기)    | 체결 후 포트폴리오 업데이트, 알림 발송, AI 분석 요청 |

FeignClient 호출에는 timeout을 명시하고 Resilience4j Circuit Breaker + Fallback을 적용합니다.

 <br>

## 핵심 비즈니스 로직

***

<details>
<summary> 👤 유저 서비스</summary>

`이메일 회원가입 / 로그인`

- 이메일과 비밀번호로 회원가입하고 로그인 할 수 있습니다.

`로그아웃`

- 로그인 시 발급된 액세스 토큰이 블랙리스트 처리되며, 리프레시 토큰이 만료됩니다.

`소셜로그인`

- Google / Kakao 계정으로 로그인 할 수 있습니다.

`토큰 관리`

- 액세스 토큰 만료 시 리프레시 토큰으로 재발급 됩니다.

`내 정보`

- 닉네임, 이름, 전화번호를 수정할 수 있습니다.
- 소셜 전용 계정에 비밀번호를 추가해 통합 계정으로 전환할 수 있습니다.

`프로필 이미지`

- 이모지 또는 이미지로 프로필을 설정할 수 있습니다.

`투자 성향`

- 점수를 입력하면 `공격투자형 / 적극투자형 / 위험중립형 / 안정추구형 / 안전형` 으로 분류됩니다.

`관심사 / 관심종목`

- 관심 카테고리와 종목코드를 등록할 수 있습니다.
- 등록된 관심 정보는 타 맞춤 서비스에 활용됩니다.
</details>

<details>
<summary> 📊 실시간 시세 서비스</summary>

`KIS 정보 마스터 파일 저장`

- 한국투자증권에서 지원하는 국내 코스피, 코스닥 종목들의 정보 파일을 다운로드 받고 변환하여 DB에 저장합니다.
- 한국투자증권에서 지원하는 테마 정보 마스터 파일을 다운로드 받고 변환하여 DB에 저장합니다.

`주식 목록 조회`

- 주식 정보를 페이징 처리된 목록으로 조회합니다.
- 조회한 주식 종목은 WebSocket 구독 대상에 추가되어 실시간 시세가 저장됩니다.

`단일 주식 상세 조회`

- 단일 종목에 대한 상세 정보를 조회합니다.
- 조회한 주식은 WebSocket으로 구독되어 실시간으로 저장됩니다.

`다중 주식 상세 조회`

- 다중 종목에 대한 상세 정보를 조회합니다.
- 조회할 종목에 대한 식별 코드를 입력 받아 반환합니다.
- 조회한 주식은 WebSocket으로 구독되어 실시간으로 저장됩니다.

`주식 분봉 조회`

- 단일 주식 종목에 대한 당일 분봉 정보를 제공합니다.
- 분봉은 1,3,5,10분 단위까지 지원하며 KIS에서 제공하는 일분봉조회를 다중 조회하여 서버에서 계산해서 반환합니다.

`테마별 거래량 조회`

- 테마 마스터 파일을 변환하여 저장한 정보를 바탕으로 거래량 상위 5개 테마를 조회합니다.
- 조회는 스케줄러로 1분마다 갱신되며 조회된 정보는 캐싱됩니다.

`단일 종목 거래량 조회`

- 단일 종목들에 대하여 거래량 상위 5개 종목을 조회합니다.
- 단일 종목은 거래량의 변화가 클 수 있기 떄문에 캐싱하지 않고 즉각 조회로 최신성을 보장합니다.

</details>

<details>
<summary> 🏦 모의 매수/매도 및 개인 주식 포트폴리오 분석</summary>

`모의 매수`

- 현재 주가(서버 시간)를 기준으로 모의 매수를 진행합니다.
- 모의 계좌 금액 이내로 구매가 가능하며, 소수점 매수를 지원합니다.

`모의 매도`

- 현재 주가(서버 시간)를 기준으로 모의 매도를 진행합니다.
- 정규장 운영 시간에만 매수/매도가 가능합니다.

`자산 및 보유 종목 현황 조회`

- 매수/매도 거래 이력을 반영해 사용자의 현재 예수금과 전체 자산 성과를 계산합니다.
- 보유 종목 기준의 평가 손익과 수익률을 별도로 제공해 종목별 성과를 확인할 수 있습니다.

`포트폴리오 AI 분석`

- 사용자의 자산 현황, 보유 종목, 투자 성향을 기반으로 AI 포트폴리오 분석을 요청할 수 있습니다.
- 사용자의 투자 성향과 현재 포트폴리오 위험도를 비교해 적합도 점수를 제공합니다.
- 무료 사용자는 AI 포트폴리오 분석을 최대 5회까지 이용할 수 있으며, 모든 사용자는 하루 1회만 요청 가능합니다.

</details>

<details>
<summary> 📰 기업 뉴스 AI 분석</summary>

`뉴스 수집 파이프라인`

- Naver News API로 기업과 거시 시장 뉴스를 수집합니다.
- 필터링을 적용하여, 정확도가 높은 뉴스를 수집합니다.

`뉴스 검색 기능`

- 수집된 뉴스를 검색할 수 있습니다.
- 회사명, 키워드, 날짜 등으로 검색할 수 있습니다.

`기업 이슈 분석`

- 특정 기업에 대한 이슈 분석과 주가 전망을 AI가 제공합니다.
- 일정 시간 동안 모두 같은 정보로 제공됩니다.

`거시 시장 뉴스 분석`

- 거시 시장(달러, 코스피, 전쟁 등)에 대한 뉴스를 AI가 분석하여 제공합니다.
- 일정 시간 동안 모두 같은 정보로 제공됩니다.

</details>

<details>
<summary> 💡 결제 및 알림</summary>

`AI 서비스 정기 구독`

- Toss 결제로 AI 포트폴리오 서비스를 정기 구독할 수 있습니다.
- 결제일 기준 30일마다 자동 결제 됩니다.

`정기 구독 해지`

- 원할 때 정기 구독을 해지할 수 있습니다.
- 구독을 해지해도 구독 만료일 이전까지 동일하게 서비스를 사용할 수 있습니다.

`결제 내역 조회`

- 모든 결제 내역을 조회할 수 있습니다.

`구독 상태 조회`

- 현재 구독 중인지, 다음 결제일은 언제인지 등을 조회할 수 있습니다.

`구독 재활성`

- 정기 구독 중 계좌 잔고 부족 등으로 구독이 정지될 수 있습니다.
- 이때 다시 재활성화할 수 있습니다.

`장 오픈/마감 알람`

- 유저는 로그인 후 서비스에 접속해 있으면 해당 시간에 알람을 받습니다.

</details>



 <br>

## ERD

***

![ERD](docs/images/erd-diagram.png)

 <br>

## API 명세서

***

> **공통 사항**
> - Base URL: `http://localhost:8080/api/v1`
> - 인증: JWT (`Authorization: Bearer {token}`), 회원가입·로그인 API 제외
> - Content-Type: `application/json`

| 서비스                  | API 명세                                                                                                                                                                                                                                |
|:---------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| User Service         | [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/NaejusikSamjo/moni/develop/docs/swagger/user-service.json)         |
| Trade Service        | [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/NaejusikSamjo/moni/develop/docs/swagger/trade-service.json)        |
| Stock Service        | [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/NaejusikSamjo/moni/develop/docs/swagger/stock-service.json)        |
| Portfolio Service    | [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/NaejusikSamjo/moni/develop/docs/swagger/portfolio-service.json)    |
| Notification Service | [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/NaejusikSamjo/moni/develop/docs/swagger/notification-service.json) |
| Payment Service      | [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/NaejusikSamjo/moni/develop/docs/swagger/payment-service.json)      |
| AI Service           | [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/NaejusikSamjo/moni/develop/docs/swagger/ai-service.json)           |

 <br>

## 인프라 아키텍처

***

![인프라 다이어그램](docs/images/infra-diagram.png)

### Compose 파일 분리 구조

앱 재배포·핫픽스 적용 시 인프라(DB·Redis·Kafka)를 재시작하지 않도록 파일을 분리합니다.

| 파일                                | 역할                                                                             |
|:----------------------------------|:-------------------------------------------------------------------------------|
| `docker-compose.infra.yml`        | DB(PostgreSQL), Redis, Redis Cluster, Kafka, Zookeeper                         |
| `docker-compose.yml`              | 애플리케이션 서비스 — 로컬 개발용 (소스 빌드 기반)                                                 |
| `docker-compose.service.prod.yml` | 운영 배포용 — ECR 이미지 + OTEL javaagent + prod profile (`docker-compose.yml`과 함께 사용) |
| `docker-compose.monitor.yml`      | Tempo · Loki · Promtail · Prometheus · Grafana — 모니터링 전용 EC2에서 실행              |
| `docker-compose.alloy.yml`        | Grafana Alloy — 메트릭·로그·트레이스 수집, Loki·Prometheus·Tempo로 전송 (운영용)                |

 <br>

## CI/CD

***

![CI/CD Flow](docs/images/CI_CD-flow.png)

GitHub Actions 기반 파이프라인이 구성되어 있으며, 결과는 Discord 알림으로 수신합니다.

자세한 내용은 [배포 가이드 문서](docs/guides/deployment.md)를 확인하세요.

 <br>

## 실행 방법

***

### 사전 요구사항

- Java 21
- Docker & Docker Compose

### 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다. `.env.example`을 참고하세요.

```env
# --- AWS ---
CLOUDFRONT_DOMAIN=

# --- Database (서비스별 독립 DB) ---
USER_DB_USERNAME=          USER_DB_PASSWORD=          USER_DB_URL=
PORTFOLIO_DB_USERNAME=     PORTFOLIO_DB_PASSWORD=     PORTFOLIO_DB_URL=
STOCK_DB_USERNAME=         STOCK_DB_PASSWORD=         STOCK_DB_URL=
NOTIFICATION_DB_USERNAME=  NOTIFICATION_DB_PASSWORD=  NOTIFICATION_DB_URL=
TRADE_DB_USERNAME=         TRADE_DB_PASSWORD=         TRADE_DB_URL=
PAYMENT_DB_USERNAME=       PAYMENT_DB_PASSWORD=       PAYMENT_DB_URL=
AI_DB_USERNAME=            AI_DB_PASSWORD=            AI_DB_URL=

# --- Redis ---
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# --- Redis Cluster (stock-service) ---
REDIS_CLUSTER_HOST=
REDIS_CLUSTER_PORT_0=
REDIS_CLUSTER_PORT_1=
REDIS_CLUSTER_PORT_2=
REDIS_CLUSTER_PASSWORD=
REDIS_NODES=

# --- Kafka ---
KAFKA_HOST_IP=
KAFKA_PORT=
ZOOKEEPER_PORT=
KAFKA_BOOTSTRAP_SERVERS=

# --- JWT ---
JWT_SECRET_KEY=
JWT_ACCESS_TOKEN_EXPIRATION=
JWT_REFRESH_TOKEN_EXPIRATION=
GATEWAY_SECRET=

# --- OAuth2 (Google / Kakao) ---
GOOGLE_CLIENT_ID=          GOOGLE_CLIENT_SECRET=      GOOGLE_REDIRECT_URI=
KAKAO_CLIENT_ID=           KAKAO_CLIENT_SECRET=       KAKAO_REDIRECT_URI=
CORS_ALLOWED_ORIGINS=

# --- Okta (admin-service) ---
OKTA_CLIENT_ID=
OKTA_CLIENT_SECRET=
OKTA_ISSUER_URI=

# --- Okta (Grafana SSO) ---
OKTA_GRAFANA_CLIENT_ID=
OKTA_GRAFANA_CLIENT_SECRET=
OKTA_GRAFANA_ISSUER_URI=

# --- Observability ---
OTEL_EXPORTER_OTLP_ENDPOINT=   # 로컬: http://localhost:4317 / 운영: monitor EC2 private IP:4317
TEMPO_GRPC_PORT=
LOKI_PORT=
PROMETHEUS_PORT=
GRAFANA_PORT=
GRAFANA_ADMIN_USER=
GRAFANA_ADMIN_PASSWORD=
GRAFANA_ROOT_URL=

# --- Infrastructure ---
SERVICE_PRIVATE_IP=
INFRA_PRIVATE_IP=
ECR_REGISTRY=

# --- External API ---
NAVER_CLIENT_ID=        NAVER_CLIENT_SECRET=        NAVER_BASE_URL=
GEMINI_API_KEY=
OPENAI_API_KEY=         OPENAI_EMBEDDING_MODEL=
KIS_APP_KEY=            KIS_APP_SECRET=             KIS_WS_TR_ID=
TOSS_SECRET_KEY=
```

### 실행 순서

```bash
# 1. 저장소 클론
git clone https://github.com/NaejusikSamjo/moni.git
cd moni

# 2. .env 파일 생성 (위 내용 참고)

# 3. 인프라 먼저 시작 (DB, Redis Cluster, Kafka)
docker compose -f docker-compose.infra.yml up -d

# 4. Gradle 빌드 (Docker 이미지 생성 전 필수)
./gradlew clean build -x test

# 5. 앱 서비스 시작
docker compose up -d --build

# 6. 시작 순서 (healthcheck 기반 자동 관리)
# 1단계: config-server
# 2단계: eureka-server
# 3단계: api-gateway + 도메인 서비스 (user, trade, stock, portfolio, notification, payment, ai, admin)

# 7. 종료
docker compose down
docker compose -f docker-compose.infra.yml down
```


