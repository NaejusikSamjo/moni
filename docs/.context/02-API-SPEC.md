# 02. API 명세

> 원본: 노션 "API" 데이터베이스(서비스별 표)를 정리. 모든 API는 "진행 전" 상태이며,
> 아직 구현된 엔드포인트는 없습니다. 신규 컨트롤러 작성 시 이 표의 URL/Method를 그대로
> 사용하세요(불일치 발견 시 사용자에게 먼저 확인).

---

## 1. USER (user-service, 담당: 동원)

| 기능                    | URL                                      | Method | 비고                                                                            |
|-----------------------|------------------------------------------|--------|-------------------------------------------------------------------------------|
| 회원가입                  | `/api/v1/auth/signup`                    | POST   |                                                                               |
| 로그인                   | `/api/v1/auth/login`                     | POST   |                                                                               |
| 로그아웃                  | `/api/v1/auth/logout`                    | POST   |                                                                               |
| 토큰 재발급                | `/api/v1/auth/refresh`                   | POST   |                                                                               |
| 소셜 로그인 URL 생성         | `/api/v1/auth/social/login-url`          | POST   | Body: provider, codeChallenge, state — 프론트가 생성한 PKCE 값을 받아 OAuth URL 반환       |
| 소셜 로그인 (Google/Kakao) | `/api/v1/auth/social/login`              | POST   | Body: provider, code, codeVerifier — Authorization Code + PKCE 흐름, 서비스 JWT 발급 |
| 내 정보 수정               | `/api/v1/users/me`                       | PATCH  |                                                                               |
| 내 정보 조회               | `/api/v1/users/me`                       | GET    |                                                                               |
| 투자 성향 등록              | `/api/v1/users/me/tendency`              | POST   |                                                                               |
| 투자 성향 조회              | `/api/v1/users/me/tendency`              | GET    |                                                                               |
| 투자 성향 수정              | `/api/v1/users/me/tendency`              | PUT    |                                                                               |
| 관심사 등록                | `/api/v1/users/me/interests`             | POST   |                                                                               |
| 관심사 조회                | `/api/v1/users/me/interests`             | GET    |                                                                               |
| 관심사 수정                | `/api/v1/users/me/interests`             | PUT    |                                                                               |
| 관심종목 추가               | `/api/v1/users/me/watchlist/{stockCode}` | PUT    |                                                                               |
| 관심종목 삭제               | `/api/v1/users/me/watchlist/{stockCode}` | DELETE |                                                                               |
| 관심종목 목록 조회            | `/api/v1/users/me/watchlist`             | GET    |                                                                               |

---

## 2. 모의 투자 서비스 (trade-service, 담당: 동민)

| 기능              | URL                                 | Method | 비고               |
|-----------------|-------------------------------------|--------|------------------|
| 주식 매수           | `/api/v1/trades/buy`                | POST   |                  |
| 주식 매도           | `/api/v1/trades/sell`               | POST   |                  |
| 거래 내역 조회        | `/api/v1/trades/history`            | GET    |                  |
| 거래 내역 상세 조회     | `/api/v1/trades/history/{tradeId}`  | GET    |                  |
| 보유 종목 조회        | `/api/v1/holdings`                  | GET    |                  |
| 보유 종목 현황 조회     | `/api/v1/holdings/{ticker}`         | GET    |                  |
| 가상 계좌 생성        | `/api/v1/accounts`                  | POST   |                  |
| 내 가상 자산 잔액 조회   | `/api/v1/accounts/me`               | GET    |                  |
| 자동 주문 설정 목록 조회  | `/api/v1/auto-orders`               | GET    | P2 도전: 자동 손절/익절  |
| 자동 손절/익절 설정 등록  | `/api/v1/auto-orders`               | POST   | P2 도전            |
| 자동 주문 목표가/수량 수정 | `/api/v1/auto-orders/{autoOrderId}` | PATCH  | P2 도전            |
| 자동 주문 설정 삭제     | `/api/v1/auto-orders/{autoOrderId}` | DELETE | P2 도전            |
| 주식 모으기 설정 목록 조회 | `/api/v1/saving-plans`              | GET    | P2 도전: 주식 모으기    |
| 주식 모으기 설정 등록    | `/api/v1/saving-plans`              | POST   | P2 도전            |
| 모으기 금액/주기 수정    | `/api/v1/saving-plans/{planId}`     | PATCH  | P2 도전            |
| 주식 모으기 설정 삭제    | `/api/v1/saving-plans/{planId}`     | DELETE | P2 도전            |
| 전체 수익 랭킹 조회     | `/api/v1/rankings`                  | GET    | P2 도전: 사용자 수익 랭킹 |
| 내 순위 조회         | `/api/v1/rankings/me`               | GET    | P2 도전            |

---

## 3. 투자 종목 조회 / 시세 조회 (stock-service, 담당: 영욱)

| 기능                 | URL                             | Method | 비고                                   |
|--------------------|---------------------------------|--------|--------------------------------------|
| 종목 검색 조회           | `/api/v1/stocks/search`         | GET    |                                      |
| 종목 목록 조회           | `/api/v1/stocks/search`         | GET    | 검색과 동일 URL로 기재됨 — 파라미터로 구분할지 사용자와 확인 |
| 단일 종목 상세 조회        | `/api/v1/stocks/{ticker}`       | GET    |                                      |
| 차트 조회              | `/api/v1/stocks/{ticker}/chart` | GET    |                                      |
| 실시간 인기 테마 조회       | `/api/v1/themes`                | GET    |                                      |
| 실시간 거래량 상위 Top5 조회 | `/api/v1/stocks/top-volume`     | GET    |                                      |

---

## 4. 포트폴리오 (portfolio-service, 담당: 설아)

| 기능             | URL                                                  | Method | 비고                                                                               |
|----------------|------------------------------------------------------|--------|----------------------------------------------------------------------------------|
| 보유 종목 현황 조회    | `/api/v1/portfolio/holdings`                         | GET    |                                                                                  |
| 수익률 계산         | `/api/v1/portfolio/returns`                          | GET    |                                                                                  |
| 종목별 손익         | `/api/v1/portfolio/holdings/{stockCode}/profit-loss` | GET    |                                                                                  |
| 자산 조회          | `/api/v1/portfolio/assets`                           | GET    |                                                                                  |
| AI 포트폴리오 분석 요청 | `/api/v1/portfolio/ai-analysis`                      | POST   | 내부적으로 ai-service의 `/api/v1/ai/portfolio/analysis` 호출(Feign, Authorization 헤더 전달) |
| AI 포트폴리오 분석 조회 | `/api/v1/portfolio/ai-analysis/latest`               | GET    |                                                                                  |

---

## 5. AI 서비스 (ai-service, 담당: 지은/설아)

| 기능          | URL                                            | Method | 비고                                                             |
|-------------|------------------------------------------------|--------|----------------------------------------------------------------|
| 기업 이슈 분석    | `/api/v1/ai/stocks/{stockCode}/issue-analysis` | GET    |                                                                |
| 뉴스 요약 조회    | `/api/v1/ai/stocks/{stockCode}/news-summary`   | GET    |                                                                |
| 포트폴리오 AI 분석 | `/api/v1/ai/portfolio/analysis`                | POST   | header: `Authorization` 필요. portfolio-service가 호출하는 내부 API     |
| 뉴스 fetch    | `/api/v1/ai/news/fetch`                        | POST   | 스케줄러 또는 매니저용. body 예: `{"stockCode": "..."}` → 특정 종목 뉴스만 fetch |

---

## 6. 결제 (payment-service, 담당: 혜수)

| 기능         | URL                            | Method | 비고                                                                                                           |
|------------|--------------------------------|--------|--------------------------------------------------------------------------------------------------------------|
| 아임포트 구독 결제 | `/api/v1/payment/subscription` | POST   |                                                                                                              |
| 구독 해제      | `/api/v1/payments`             | POST   | 원본 표 그대로 — URL이 결제 내역과 동일 prefix인데 method가 POST. 구현 시 `/api/v1/payment/subscription` (DELETE 등)으로 통일할지 확인 필요 |
| 결제 내역 조회   | `/api/v1/payments/history`     | GET    |                                                                                                              |
| 구독 상태 조회   | `/api/v1/payment/status`       | GET    |                                                                                                              |

> ⚠️ 결제 관련 URL은 `payment`(단수)와 `payments`(복수)가 혼재되어 있습니다.
> 구현 전에 사용자와 합의해서 한 가지로 통일하는 것을 권장합니다.

---

## 7. 알림 (notification-service, 담당: 혜수)

원본 노션 문서에 별도 API 표가 작성되지 않았습니다. P1 "사용자 맞춤 알림 서비스(ex. 미장 개장 10분 전)"
구현 시 API 설계가 필요하면 사용자에게 요청 패턴(폴링 vs 웹소켓/SSE vs FCM push 등)을 먼저 확인하세요.
