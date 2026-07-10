# 03. ERD (서비스별 테이블 스키마)

> 원본: 노션 ERD 페이지의 테이블 정의를 정리. 엔티티/리포지토리/마이그레이션 작성 시
> 이 문서를 기준으로 하되, 컬럼명·타입에 대해 의문이 있으면 사용자에게 확인하세요.
> 모든 서비스 DB는 PostgreSQL이며(`mysql → postgredb`로 변경됨), 서비스별로 DB가 분리되어 있습니다.

---

## 0. 공통 감사 필드 (Base Audit Fields)

모든 테이블은 아래 공통 감사 필드를 포함합니다. `common` 모듈에 `BaseEntity`(또는 `BaseAuditEntity`)로
추출해 `@MappedSuperclass` + `AuditingEntityListener` 등으로 재사용하는 것을 권장합니다.

| 컬럼명        | 데이터 타입       | 제약 조건    | 설명    |
|------------|--------------|----------|-------|
| created_at | TIMESTAMP    | Not Null | 생성 시점 |
| created_by | VARCHAR(100) | Not Null | 생성자   |
| updated_at | TIMESTAMP    |          | 수정 시점 |
| updated_by | VARCHAR(100) |          | 수정자   |
| deleted_at | TIMESTAMP    |          | 삭제 시점 |
| deleted_by | VARCHAR(100) |          | 삭제자   |

---

## 1. User-Service

### `p_users` — 사용자 공통정보

| 컬럼명              | 데이터 타입       | 제약 조건                      | 설명                                |
|------------------|--------------|----------------------------|-----------------------------------|
| id               | VARCHAR(36)  | PK, Not Null               | 사용자 고유 식별자 (UUID)                 |
| email            | VARCHAR(100) | Unique, Not Null           | 로그인 계정 (이메일)                      |
| password         | VARCHAR(255) | Nullable                   | 비밀번호 (OAuth 시 null)               |
| name             | VARCHAR(50)  | Not Null                   | 사용자 실명                            |
| nickname         | VARCHAR(50)  | Not Null                   | 자동 생성 닉네임                         |
| phone            | VARCHAR(20)  | Nullable                   | 연락처                               |
| oauth_provider   | VARCHAR(20)  | Nullable                   | OAuth 제공자 (google / kakao)        |
| oauth_id         | VARCHAR(255) | Nullable                   | OAuth 제공자 ID                      |
| role             | VARCHAR(10)  | Not Null, Default 'USER'   | 권한 (USER / ADMIN)                 |
| status           | VARCHAR(10)  | Not Null, Default 'ACTIVE' | 상태 (ACTIVE / SUSPENDED / DELETED) |
| suspended_reason | TEXT         | Nullable                   | 정지 사유                             |
| deleted_reason   | TEXT         | Nullable                   | 탈퇴/삭제 사유                          |
| + 공통 감사 필드       |              |                            |                                   |

### `p_tendency` — 투자 성향

| 컬럼명     | 데이터 타입      | 제약 조건        | 설명                                          |
|---------|-------------|--------------|---------------------------------------------|
| id      | VARCHAR(36) | PK, Not Null | 성향 고유 식별자 (UUID)                            |
| user_id | VARCHAR(36) | FK, Not Null | `users.id` 참조                               |
| score   | INT         | Not Null     | 설문 총점                                       |
| type    | VARCHAR(20) | Not Null     | 성향 타입 (공격투자형 / 적극투자형 / 위험중립형 / 안정추구형 / 안전형) |

### `p_interests` — 관심사

| 컬럼명      | 데이터 타입      | 제약 조건        | 설명                     |
|----------|-------------|--------------|------------------------|
| id       | VARCHAR(36) | PK, Not Null | 관심사 고유 식별자 (UUID)      |
| user_id  | VARCHAR(36) | FK, Not Null | `users.id` 참조          |
| category | VARCHAR(50) | Not Null     | 관심사 종류 (IT, 금융, 커머스 등) |

### `p_watchlist` — 관심종목

| 컬럼명        | 데이터 타입      | 제약 조건        | 설명                 |
|------------|-------------|--------------|--------------------|
| id         | VARCHAR(36) | PK, Not Null | 즐겨찾기 고유 식별자 (UUID) |
| user_id    | VARCHAR(36) | FK, Not Null | `users.id` 참조      |
| stock_code | VARCHAR(20) | Not Null     | 종목 코드              |

---

## 2. Stock-Service

### `stock` — 주식 종목

| 컬럼명    | 데이터 타입       | 제약 조건        | 설명      |
|--------|--------------|--------------|---------|
| id     | UUID         | PK, Not Null | 주식 ID   |
| ticker | VARCHAR(10)  | Not Null     | 주식 코드   |
| name   | VARCHAR(100) | Not Null     | 사명      |
| market | VARCHAR(10)  | Not Null     | 소속 시장명  |
| per    | DECIMAL(6,2) |              | PER 값   |
| pbr    | DECIMAL(6,2) |              | PBR 값   |
| high52 | BIGINT       |              | 52주 신고가 |
| low52  | BIGINT       |              | 52주 신저가 |

### `theme` — 테마

| 컬럼명        | 데이터 타입       | 제약 조건        | 설명    |
|------------|--------------|--------------|-------|
| id         | UUID         | PK, Not Null | 테마 ID |
| theme_code | VARCHAR(20)  | Not Null     | 테마 코드 |
| theme_name | VARCHAR(100) | Not Null     | 테마명   |

### `stock_theme` — 주식 소속 테마 (N:M 연결 테이블)

| 컬럼명      | 데이터 타입 | 제약 조건        | 설명             |
|----------|--------|--------------|----------------|
| id       | UUID   | PK, Not Null | 테마 매핑 ID       |
| stock_id | UUID   | FK, Not Null | `stock.id` 외래키 |
| theme_id | UUID   | FK, Not Null | `theme.id` 외래키 |

> 실시간 시세(현재가/등락률/거래량 등)는 위 정적 테이블이 아니라 Redis String으로 관리합니다
> (`StockPriceRedisAdapter`, 키 `stock:price:{ticker}` TTL 10초, `stock:top-volume` TTL 1분,
> JSON 직렬화된 값 저장). Kafka 버퍼·TimescaleDB·S3 Parquet/Athena 구조는 도입하지 않았습니다.

---

## 3. Payment-Service (Toss Payments 연동)

### `p_payment` — 결제

| 컬럼명            | 데이터 타입      | 제약 조건            | 설명                                                                   |
|----------------|-------------|------------------|----------------------------------------------------------------------|
| id             | UUID        | PK, Not Null     | 결제 고유 아이디                                                            |
| merchant_id    | VARCHAR(64) | Unique, Not Null | 가맹점 주문 번호 (`MerchantIdConverter`)                                     |
| user_id        | UUID        | Not Null         | 결제자 ID (user-service 참조)                                             |
| payment_type   | VARCHAR(30) | Not Null         | `SUBSCRIPTION_INITIAL` / `SUBSCRIPTION_RECURRING` / `SUBSCRIPTION_REACTIVATION` |
| amount         | DECIMAL(19,4) | Not Null       | 결제 금액 (`MoneyConverter`)                                             |
| pg_payment_key | VARCHAR     | Unique           | Toss 결제 키                                                            |
| status         | VARCHAR(20) | Not Null         | `PENDING` / `COMPLETED` / `FAILED`                                    |
| expires_at     | TIMESTAMP   | Not Null         | 결제 만료 시각                                                             |
| + 공통 감사 필드 (created/updated) |   |                  |                                                                        |

### `p_payment_history` — 결제 상태 이력

| 컬럼명          | 데이터 타입      | 제약 조건        | 설명                |
|--------------|-------------|--------------|-------------------|
| id           | UUID        | PK, Not Null | 이력 고유 아이디         |
| payment_id   | UUID        | FK, Not Null | `p_payment.id` 참조 |
| from_status  | VARCHAR(20) | Not Null     | 이전 상태             |
| to_status    | VARCHAR(20) | Not Null     | 이후 상태             |
| pg_response  | TEXT        |              | PG 응답 원문          |
| requested_at | TIMESTAMP   | Not Null     | 요청 시각             |
| requested_by | VARCHAR(100) | Not Null    | 요청자               |
| responded_at | TIMESTAMP   |              | 응답 시각             |

### `p_subscription` — 정기 구독

| 컬럼명                  | 데이터 타입        | 제약 조건        | 설명                                                                                     |
|----------------------|---------------|--------------|----------------------------------------------------------------------------------------|
| id                   | UUID          | PK, Not Null | 구독 고유 아이디                                                                              |
| user_id              | UUID          | Not Null     | 구독자 ID                                                                                  |
| billing_key          | VARCHAR(50)   |              | Toss 빌링키 (`BillingKeyConverter`로 컬럼에 직접 저장 — 별도 테이블 없음)                                  |
| amount               | DECIMAL(19,4) |            | 구독 금액                                                                                   |
| status               | VARCHAR(30)   | Not Null     | `PENDING_ACTIVATION` / `ACTIVE` / `CANCELLING` / `CANCELLED` / `SUSPENDED`              |
| next_billing_date    | DATE          |              | 다음 결제일                                                                                  |
| billing_key_deleted_at | TIMESTAMP     |              | 빌링키 삭제(해지) 시각                                                                           |
| retry_count          | INT           | Not Null     | 결제 실패 재시도 횟수                                                                            |
| version              | BIGINT        |              | 낙관적 락(`@Version`)                                                                       |
| + 공통 감사 필드 (created/updated) |               |              |                                                                                          |

### `p_subscription_history` — 구독 상태 이력

| 컬럼명            | 데이터 타입      | 제약 조건        | 설명                     |
|----------------|-------------|--------------|------------------------|
| id             | UUID        | PK, Not Null | 이력 고유 아이디              |
| subscription_id| UUID        | FK, Not Null | `p_subscription.id` 참조 |
| from_status    | VARCHAR(30) | Not Null     | 이전 상태                  |
| to_status      | VARCHAR(30) | Not Null     | 이후 상태                  |
| reason         | TEXT        |              | 상태 변경 사유               |
| changed_at     | TIMESTAMP   | Not Null     | 변경 시각                  |

---

## 4. AI-Service

### `news` — 뉴스

| 컬럼명          | 데이터 타입       | 제약 조건        | 설명                                               |
|--------------|--------------|--------------|--------------------------------------------------|
| id           | VARCHAR(36)  | PK, Not Null |                                                  |
| ticker       | VARCHAR(10)  |              | 종목코드                                             |
| title        | VARCHAR(255) |              | 뉴스 제목                                            |
| content      | TEXT         |              | 뉴스 내용                                            |
| source       | VARCHAR(10)  |              | 출처(언론사 등)                                        |
| url          | VARCHAR(255) |              | 뉴스 URL                                           |
| published_at | TIMESTAMP    |              | 뉴스 발행일                                           |
| + base audit |              |              | `created_at, created_by, deleted_at, deleted_by` |

### `p_market_news` — 거시 시장 뉴스

| 컬럼명          | 데이터 타입        | 제약 조건        | 설명                                                                       |
|--------------|---------------|--------------|--------------------------------------------------------------------------|
| id           | VARCHAR(36)   | PK, Not Null |                                                                          |
| title        | VARCHAR(255)  |              | 뉴스 제목                                                                    |
| keyword      | VARCHAR(50)   |              | 키워드 (ex: 달러, 연준 등)                                                       |
| content      | TEXT          |              | 뉴스 내용                                                                    |
| source       | VARCHAR(50)   |              | 출처 (언론사 등)                                                               |
| url          | VARCHAR(255)  |              | 뉴스 URL                                                                   |
| published_at | TIMESTAMP     |              | 뉴스 발행일                                                                   |
| + base audit |               |              | `created_at, created_by, updated_at, updated_by, deleted_at, deleted_by` |

### `p_market_news_analysis` — 거시 시장 분석

| 컬럼명          | 데이터 타입      | 제약 조건        | 설명                                                                       |
|--------------|-------------|--------------|--------------------------------------------------------------------------|
| id           | VARCHAR(36) | PK, Not Null |                                                                          |
| keyword      | VARCHAR(50) |              |                                                                          |
| summary      | TEXT        |              | 뉴스 요약                                                                    |
| expired_at   | TIMESTAMP   |              | 분석 유효 기간                                                                 |
| + base audit |             |              | `created_at, created_by, updated_at, updated_by, deleted_at, deleted_by` |

### `p_company_issue_analysis` — 기업 이슈 분석

| 컬럼명          | 데이터 타입      | 제약 조건        | 설명                                               |
|--------------|-------------|--------------|--------------------------------------------------|
| id           | VARCHAR(36) | PK, Not Null |                                                  |
| ticker       | VARCHAR(10) |              | 종목코드                                             |
| summary      | TEXT        |              | 이슈 요약                                            |
| sentiment    | VARCHAR(10) |              | 평가: `POSITIVE` / `NEGATIVE` / `NEUTRAL`          |
| expired_at   | TIMESTAMP   |              | 분석 유효 기간                                         |
| + base audit |             |              | `created_at, created_by, deleted_at, deleted_by` |

### `p_ai_log` — ai 프롬프트 저장

| 컬럼명                     | 데이터  타입     | 제약  조건       | 설명                                               |
|-------------------------|-------------|--------------|--------------------------------------------------|
| id                      | VARCHAR(36) | PK, Not Null |                                                  |
| company_analysis_id     | VARCHAR(36) | FK           | 기업 이슈 분석 id                                      |
| market_news_analysis_id | VARCHAR(36) | FK           | 거시시장 분석 id                                       |
| prompt                  | TEXT        |              | 작성 프롬프트                                          |
| + base audit            |             |              | `created_at, created_by, deleted_at, deleted_by` |

---

## 5. Portfolio-Service

> `GET /api/v1/assets/analysis-snapshot`). 소유·정합성은 전적으로 trade-service(6번 섹션)에 있습니다.

### `portfolio` — 포트폴리오

| 컬럼명                         | 데이터 타입        | 제약 조건               | 설명          |
|-----------------------------|---------------|---------------------|-------------|
| id                          | UUID          | PK, Not Null        | 포트폴리오 ID    |
| user_id                     | VARCHAR(36)   | FK, Not Null        | 사용자 ID      |
| profit                      | DECIMAL(18,2) | Not Null            | 손익 금액 (캐시)  |
| profit_rate                 | DECIMAL(8,4)  | Not Null            | 수익률 (캐시)    |
| ai_analysis_count           | BIGINT        | Not Null, Default 0 | AI 분석 사용 횟수 |
| + 공통 감사 필드 (타입 VARCHAR(36)) |               |                     |             |

### `portfolio_analysis` — AI 포트폴리오 분석

| 컬럼명                     | 데이터 타입        | 제약 조건                   | 설명                    |
|-------------------------|---------------|-------------------------|-----------------------|
| id                      | UUID          | PK, Not Null            | 분석 ID                 |
| portfolio_id            | UUID          | FK, Not Null            | `portfolio.id` 참조     |
| total_return_rate       | DECIMAL(7,4)  | Not Null                | 분석 시점 손익률 (%)         |
| total_evaluation_amount | DECIMAL(18,2) | Not Null                | 총 평가금액                |
| summary                 | TEXT          | Not Null                | AI 요약                 |
| concentration_score     | DECIMAL(5,2)  | Not Null                | 집중도 점수                |
| is_concentrated         | BOOLEAN       | Not Null, Default false | 집중 여부 (임계값 초과 시 true) |
| analyzed_at             | TIMESTAMP     | Not Null                | 분석 시각                 |
| deleted_at              | TIMESTAMP     |                         | 삭제 시각                 |
| deleted_by              | VARCHAR(36)   |                         | 삭제자                   |

### `portfolio_sector_analysis` — AI 분석: 섹터별 비중

| 컬럼명               | 데이터 타입        | 제약 조건                | 설명                               |
|-------------------|---------------|----------------------|----------------------------------|
| id                | UUID          | PK, Not Null         | 섹터 분석 ID                         |
| analysis_id       | UUID          | FK, UNIQUE, Not Null | `portfolio_analysis.id` 참조 (1:1) |
| sector_name       | VARCHAR(50)   | Not Null             | 섹터명                              |
| weight            | DECIMAL(5,2)  | Not Null             | 비중                               |
| evaluation_amount | DECIMAL(18,2) | Not Null             | 평가금액                             |
| deleted_at        | TIMESTAMP     |                      | 삭제 시각                            |
| deleted_by        | VARCHAR(36)   |                      | 삭제자                              |

### `p_user_subscription_status` — 사용자 구독 상태 (읽기 모델)

payment-service가 Kafka로 발행하는 구독 이벤트(`PaymentSubscriptionEventListener`가 소비)를 반영한
읽기 전용 상태 테이블입니다. AI 분석 요청 시 유료 플랜 여부 판단에 사용됩니다.

| 컬럼명             | 데이터 타입      | 제약 조건            | 설명                          |
|-----------------|-------------|------------------|-----------------------------|
| id              | UUID        | PK, Not Null     | ID (UUIDv7)                 |
| user_id         | UUID        | Unique, Not Null | 사용자 ID                      |
| subscription_id | UUID        | Not Null         | payment-service `p_subscription.id` 참조 (논리적 FK) |
| subscribed      | BOOLEAN     | Not Null         | 구독 여부                       |
| status          | VARCHAR(30) | Not Null         | 구독 상태 (payment-service와 동일 값 체계) |
| last_event_type | VARCHAR(50) | Not Null         | 마지막으로 반영한 이벤트 타입            |
| last_occurred_at| TIMESTAMP   | Not Null         | 마지막 이벤트 발생 시각 (멱등 처리 기준)     |

---

## 6. Trade-Service

### `trades` — 거래 내역 (매수/매도 거래 기록)

| 컬럼명           | 데이터 타입        | 제약 조건                    | 설명                                    |
|---------------|---------------|--------------------------|---------------------------------------|
| id            | VARCHAR(36)   | PK, Not Null             | 거래 고유 식별자 (UUID)                      |
| account_id    | VARCHAR(36)   | FK, Not Null             | 계좌 ID (`account` 테이블 참조)              |
| ticker        | VARCHAR(10)   | FK, Not Null             | 종목 코드 (stock-service 참조)              |
| trade_type    | VARCHAR(4)    | Not Null                 | 거래 유형 (`BUY` / `SELL`)                |
| quantity      | INTEGER       | Not Null                 | 거래 수량(주)                              |
| price         | DECIMAL(18,2) | Not Null                 | 거래 시점 주당 가격                           |
| total_amount  | DECIMAL(18,2) | Not Null                 | 총 거래 금액 (price × quantity)            |
| profit_amount | DECIMAL(18,2) | Nullable                 | 실현 손익 (SELL일 때만, BUY는 NULL)           |
| profit_rate   | DECIMAL(8,4)  | Nullable                 | 실현 수익률 (SELL일 때만, BUY는 NULL)          |
| status        | VARCHAR(10)   | Not Null, Default 'DONE' | 거래 상태 (`PENDING` / `DONE` / `FAILED`) |

테이블명: `p_holding`

### `holding` — 보유 종목 (보유 중인 주식 현황)

| 컬럼명           | 데이터 타입        | 제약 조건        | 설명                       |
|---------------|---------------|--------------|--------------------------|
| id            | VARCHAR(36)   | PK, Not Null | 보유 종목 고유 식별자 (UUID)      |
| account_id    | VARCHAR(36)   | FK, Not Null | 계좌 ID (`account` 테이블 참조) |
| ticker        | VARCHAR(10)   | FK, Not Null | 종목 코드 (stock-service 참조) |
| quantity      | INTEGER       | Not Null     | 보유 수량(주)                 |
| average_price | DECIMAL(18,2) | Not Null     | 평균 매수 단가                 |
| total_amount  | DECIMAL(18,2) | Not Null     | 누적 매수 금액                 |
| version       | BIGINT        |              | 낙관적 락(`@Version`)        |

테이블명: `p_account`

### `account` — 가상 계좌 (모의투자 시드머니 관리)

| 컬럼명              | 데이터 타입        | 제약 조건        | 설명                       |
|------------------|---------------|--------------|--------------------------|
| id               | VARCHAR(36)   | PK, Not Null | 계좌 고유 식별자 (UUID)         |
| user_id          | VARCHAR(36)   | FK, Not Null | 사용자 ID (user-service 참조) |
| balance          | DECIMAL(18,4) | Not Null     | 현재 보유 잔액                 |
| total_investment | DECIMAL(18,2) | Not Null     | 누적 투자 원금                 |
| version          | BIGINT        |              | 낙관적 락(`@Version`)        |

---

## 데이터 소유권 정리

- payment-service의 구독 상태는 Kafka 이벤트로 발행되고, portfolio-service의
  `p_user_subscription_status`가 이를 구독해 읽기 모델로 반영합니다(최종적 일관성).
- `p_payment.user_id`, trade-service `account.user_id` 등은 user-service `p_users.id`(UUID)를
  참조하는 **논리적 FK**입니다 (DB 레벨 FK 제약은 서비스 간에 걸 수 없으므로 애플리케이션 레벨에서
  정합성을 보장합니다).
