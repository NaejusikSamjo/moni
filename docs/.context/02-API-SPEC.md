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
| 내 정보 조회               | `/api/v1/users/me`                       | GET    |                                                                               |
| 내 정보 수정               | `/api/v1/users/me`                       | PATCH  | name, nickname, phone만 수정 가능. 비밀번호 변경은 별도 엔드포인트 사용                            |
| 비밀번호 변경               | `/api/v1/users/me/password`              | POST   | Body: currentPassword, newPassword. 현재 비밀번호 확인 후 변경                           |
| 회원 탈퇴                 | `/api/v1/users/me`                       | DELETE |                                                                               |
| 투자 성향 등록              | `/api/v1/users/me/tendency`              | POST   |                                                                               |
| 투자 성향 조회              | `/api/v1/users/me/tendency`              | GET    |                                                                               |
| 투자 성향 수정              | `/api/v1/users/me/tendency`              | PUT    |                                                                               |
| 관심사 등록                | `/api/v1/users/me/interests`             | POST   |                                                                               |
| 관심사 조회                | `/api/v1/users/me/interests`             | GET    |                                                                               |
| 관심사 수정                | `/api/v1/users/me/interests`             | PUT    |                                                                               |
| 관심종목 추가               | `/api/v1/users/me/watchlist/{stockCode}` | PUT    | stockCode: 6자리 숫자 (국내 주식 한정)                                                  |
| 관심종목 삭제               | `/api/v1/users/me/watchlist/{stockCode}` | DELETE | stockCode: 6자리 숫자 (국내 주식 한정)                                                  |
| 관심종목 목록 조회            | `/api/v1/users/me/watchlist`             | GET    |                                                                               |

---

### 1-2. admin ( **admin-service Feign Client 전용** )

> 이 엔드포인트들은 외부에서 직접 호출하지 않습니다. admin-service의 `UserAdminClient`(Feign)가
> `X-Gateway-Secret` 헤더를 포함해 호출하는 내부 전용 API입니다.

| 기능           | URL                                      | Method | 비고                                 |
|--------------|------------------------------------------|--------|------------------------------------|
| 유저 목록 조회     | `/api/v1/admin/users`                    | GET    | Pageable (size=10, createdAt DESC) |
| 삭제된 유저 목록 조회 | `/api/v1/admin/users/deleted`            | GET    | Pageable (size=10, deletedAt DESC) |
| 유저 계정 정지     | `/api/v1/admin/users/{userId}/suspend`   | PATCH  | Body: reason                       |
| 유저 계정 정지 해지  | `/api/v1/admin/users/{userId}/unsuspend` | PATCH  |                                    |
| 유저 계정 삭제     | `/api/v1/admin/users/{userId}`           | DELETE | 소프트 삭제                             |

---

## 1-3. admin-service (관리자 웹 UI, 포트: 19097, 담당: 동원)

> Thymeleaf SSR 기반 관리자 웹 페이지. api-gateway를 **거치지 않고** 직접 접근합니다.
> Okta OIDC(OAuth2 Login)로 인증하며, Okta에서 초대받은 계정만 로그인 가능합니다.
> 로그아웃 시 `OidcClientInitiatedLogoutSuccessHandler`로 Okta 세션까지 함께 종료합니다.
> 향후 `admin.000.com` 서브도메인으로 분리 예정.

| 기능          | URL                               | Method | 비고                                                 |
|-------------|-----------------------------------|--------|----------------------------------------------------|
| Okta 로그인 시작 | `/oauth2/authorization/okta`      | GET    | Spring Security 자동 생성. Okta 로그인 페이지로 리다이렉트         |
| Okta 콜백     | `/login/oauth2/code/okta`         | GET    | Spring Security 자동 처리. 성공 시 `/admin/dashboard`로 이동 |
| 로그아웃        | `/admin/logout`                   | POST   | 세션 무효화, JSESSIONID 삭제, Okta 세션까지 종료                |
| 대시보드        | `/admin/dashboard`                | GET    | 총 유저 수 표시                                          |
| 유저 목록       | `/admin/users`                    | GET    | Pageable (size=10, createdAt DESC)                 |
| 삭제된 유저 목록   | `/admin/users/deleted`            | GET    | Pageable (size=10, deletedAt DESC)                 |
| 유저 계정 정지    | `/admin/users/{userId}/suspend`   | POST   | Form: reason. user-service Feign 호출                |
| 유저 계정 정지 해지 | `/admin/users/{userId}/unsuspend` | POST   | user-service Feign 호출                              |
| 유저 계정 삭제    | `/admin/users/{userId}/delete`    | POST   | 소프트 삭제. user-service Feign 호출                      |

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

| 기능             | URL                                                      | Method | 비고                                                                               |
|----------------|----------------------------------------------------------|--------|----------------------------------------------------------------------------------|
| 포트폴리오 생성       | `/api/v1/portfolio`                                      | POST   |                                                                                  |
| 보유 종목 현황 조회    | `/api/v1/portfolio/holdings`                             | GET    |                                                                                  |
| ~~수익률 계산~~     | ~~`/api/v1/portfolio/returns`~~                          | ~~GET~~  | 현재 누적 수익률은 **자산 조회**에서 제공 (필요 시 복구)                                    |
| ~~종목별 손익~~     | ~~`/api/v1/portfolio/holdings/{stockCode}/profit-loss`~~ | ~~GET~~    | **보유 종목 현황**에서 평가손익·수익률을 제공 (필요 시 복구)                               |
| 자산 조회          | `/api/v1/portfolio/assets`                               | GET    |                                                                                  |
| AI 포트폴리오 분석 요청 | `/api/v1/portfolio/ai-analysis`                          | POST   | 내부적으로 ai-service의 `/api/v1/ai/portfolio/analysis` 호출(Feign, Authorization 헤더 전달) |
| AI 포트폴리오 분석 조회 | `/api/v1/portfolio/ai-analysis/latest`                   | GET    |                                                                                  |

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
