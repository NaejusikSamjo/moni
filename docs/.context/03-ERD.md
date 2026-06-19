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

> 실시간 시세(현재가/등락률/거래량 등)는 위 정적 테이블이 아니라 **Redis Hash** (종목당 10개 필드 수준)로
> 관리하고, raw tick은 Kafka 버퍼 → 1분 단위 OHLCV 집계 컨슈머 → TimescaleDB, raw tick 원본은
> S3 Parquet → Athena 분석 구조가 AI 검증 로그에서 논의되었습니다 (수용 여부는 `06-AI-VALIDATION-LOG.md`
> 참고, 현재 "?" 상태 — 구현 전 사용자와 재확인 필요).

---

## 3. Payment-Service

### `payments` — 결제

| 컬럼명             | 데이터 타입    | 제약 조건        | 설명                       |
|-----------------|-----------|--------------|--------------------------|
| id              | UUID      | PK, Not Null | 결제 고유 아이디                |
| merchant_id     | UUID      | Not Null     | 가맹점 고유 주문 번호             |
| payment_content | enum      | Not Null     | 주문 상품 내역                 |
| payment_state   | enum      | Not Null     | 주문 상태 (준비, 완료, 실패)       |
| amount          | LONG      | Not Null     | 주문 가격                    |
| version         | LONG      | Not Null     | 주문 버전                    |
| created_at      | timestamp | Not Null     | 생성일                      |
| created_by      | string    | Not Null     | 생성자                      |
| user_id         | UUID      | FK, Not Null | 결제자 ID (user-service 참조) |

### `payment_history` — 결제 내역

| 컬럼명         | 데이터 타입    | 제약 조건        | 설명               |
|-------------|-----------|--------------|------------------|
| id          | UUID      | PK, Not Null | 결제 내역 아이디        |
| payment_id  | UUID      | FK, Not Null | `payments.id` 참조 |
| from_state  | enum      | Not Null     | 이전 상태 추적         |
| to_state    | enum      | Not Null     | 이후 상태 추적         |
| pg_response | JSONB     | Not Null     | PG 응답 기록         |
| created_at  | timestamp | Not Null     | 생성일              |
| created_by  | string    | Not Null     | 생성자              |

### `billing_keys` — 결제키

| 컬럼명                                  | 데이터 타입  | 제약 조건        | 설명          |
|--------------------------------------|---------|--------------|-------------|
| id                                   | UUID    | PK, Not Null | 결제키 고유 아이디  |
| user_id                              | UUID    | FK, Not Null | 결제자 ID      |
| pg_provider                          | enum    | Not Null     | PG 제공자      |
| pay_method                           | enum    | Not Null     | 카드 또는 간편결제  |
| card_name                            | string  | Not Null     | 카드 이름       |
| is_active                            | boolean | Not Null     | 결제 가능 상태 확인 |
| + 공통 감사 필드 (created/updated/deleted) |         | Not Null     |             |

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

### `news_embedding` — 임베딩 데이터 (벡터 DB 연결 브릿지 테이블)

| 컬럼명          | 데이터 타입      | 제약 조건        | 설명                                                                       |
|--------------|-------------|--------------|--------------------------------------------------------------------------|
| id           | VARCHAR(36) | PK, Not Null |                                                                          |
| news_id      | VARCHAR(36) | FK, Not Null | `news.id` 참조                                                             |
| vector_id    | VARCHAR(36) |              | 벡터 DB 내 vector id                                                        |
| chunk_index  | INT         |              | 청크 인덱스                                                                   |
| + base audit |             |              | `created_at, created_by, updated_at, updated_by, deleted_at, deleted_by` |

### `news_summary` — 뉴스 요약

| 컬럼명          | 데이터 타입      | 제약 조건        | 설명                                               |
|--------------|-------------|--------------|--------------------------------------------------|
| id           | VARCHAR(36) | PK, Not Null |                                                  |
| news_id      | VARCHAR(36) | FK, Not Null | `news.id` 참조                                     |
| ticker       | VARCHAR(10) |              | 종목코드                                             |
| summary      | TEXT        |              | 뉴스 요약                                            |
| sentiment    | VARCHAR(10) |              | 평가: `POSITIVE` / `NEGATIVE` / `NEUTRAL`          |
| expired_at   | TIMESTAMP   |              | 분석 유효 기간                                         |
| + base audit |             |              | `created_at, created_by, deleted_at, deleted_by` |

### `company_issue_analysis` — 기업 이슈 분석

| 컬럼명          | 데이터 타입      | 제약 조건        | 설명                                               |
|--------------|-------------|--------------|--------------------------------------------------|
| id           | VARCHAR(36) | PK, Not Null |                                                  |
| ticker       | VARCHAR(10) |              | 종목코드                                             |
| summary      | TEXT        |              | 이슈 요약                                            |
| sentiment    | VARCHAR(10) |              | 평가: `POSITIVE` / `NEGATIVE` / `NEUTRAL`          |
| expired_at   | TIMESTAMP   |              | 분석 유효 기간                                         |
| + base audit |             |              | `created_at, created_by, deleted_at, deleted_by` |

---

## 5. Portfolio-Service

### `account` — 모의투자 계좌

| 컬럼명                         | 데이터 타입        | 제약 조건        | 설명       |
|-----------------------------|---------------|--------------|----------|
| id                          | BIGINT        | PK, Not Null | 계좌번호     |
| user_id                     | VARCHAR(36)   | FK, Not Null | 사용자 ID   |
| balance                     | DECIMAL(18,4) | Not Null     | 보유 잔액    |
| total_investment            | DECIMAL(18,2) | Not Null     | 누적 투자 원금 |
| + 공통 감사 필드 (타입 VARCHAR(36)) |               |              |          |

### `holding` — 보유 종목

| 컬럼명                         | 데이터 타입        | 제약 조건        | 설명              |
|-----------------------------|---------------|--------------|-----------------|
| id                          | UUID          | PK, Not Null | 보유 종목 ID        |
| account_id                  | BIGINT        | FK, Not Null | `account.id` 참조 |
| ticker                      | VARCHAR(10)   | Not Null     | 주식 코드           |
| quantity                    | INTEGER       | Not Null     | 보유 수량(주)        |
| average_price               | DECIMAL(18,2) | Not Null     | 평균 매수 단가        |
| total_amount                | DECIMAL(18,2) | Not Null     | 누적 매수 금액        |
| + 공통 감사 필드 (타입 VARCHAR(36)) |               |              |                 |

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

> 원본 ERD 이미지 하단에 `id VARCHAR(36) PK Not Null` 만 있는 미완성 테이블이 하나 더 보입니다.
> 용도가 불명확하므로(예: portfolio_sector_analysis 와 연계된 또 다른 분석 하위 테이블 등)
> 구현 전 사용자에게 확인하세요.

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

### `holding` — 보유 종목 (보유 중인 주식 현황)

| 컬럼명           | 데이터 타입        | 제약 조건        | 설명                       |
|---------------|---------------|--------------|--------------------------|
| id            | VARCHAR(36)   | PK, Not Null | 보유 종목 고유 식별자 (UUID)      |
| account_id    | VARCHAR(36)   | FK, Not Null | 계좌 ID (`account` 테이블 참조) |
| ticker        | VARCHAR(10)   | FK, Not Null | 종목 코드 (stock-service 참조) |
| quantity      | INTEGER       | Not Null     | 보유 수량(주)                 |
| average_price | DECIMAL(18,2) | Not Null     | 평균 매수 단가                 |
| total_amount  | DECIMAL(18,2) | Not Null     | 누적 매수 금액                 |

### `account` — 가상 계좌 (모의투자 시드머니 관리)

| 컬럼명              | 데이터 타입        | 제약 조건        | 설명                       |
|------------------|---------------|--------------|--------------------------|
| id               | VARCHAR(36)   | PK, Not Null | 계좌 고유 식별자 (UUID)         |
| user_id          | VARCHAR(36)   | FK, Not Null | 사용자 ID (user-service 참조) |
| balance          | DECIMAL(18,4) | Not Null     | 현재 보유 잔액                 |
| total_investment | DECIMAL(18,2) | Not Null     | 누적 투자 원금                 |

---

## ⚠️ 알아두어야 할 데이터 중복/소유권 이슈

- **`account`, `holding` 테이블이 portfolio-service(섹션 5)와 trade-service(섹션 6) 양쪽에 존재**합니다.
  - trade-service의 `account`/`holding`: PK 타입이 `VARCHAR(36)`(UUID 문자열) — 매수/매도 처리의
    소스 오브 트루스로 보임.
  - portfolio-service의 `account`/`holding`: PK 타입이 `BIGINT`/`UUID` — 대시보드 표시/계산용으로
    보임.
  - 두 서비스가 **같은 테이블을 직접 공유하면 안 됩니다** (MSA 안티패턴 #2, `05-MSA-GUIDELINES.md` 참고).
    구현 전 "trade-service가 account/holding의 소유자이고, portfolio-service는 Kafka 이벤트나
    Feign 조회로 동기화/조회한다"는 방향이 맞는지 사용자에게 확인하세요.
- payment-service의 `payments.user_id`, portfolio-service/trade-service의 `user_id` 등은
  user-service의 `users.id`(UUID 문자열, VARCHAR(36))를 참조하는 **논리적 FK**입니다 (DB 레벨
  FK 제약은 서비스 간에 걸 수 없으므로 애플리케이션 레벨에서 정합성을 보장해야 합니다).
