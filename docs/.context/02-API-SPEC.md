# 02. API 명세

> 전 서비스 API가 구현 완료되어 배포되어 있습니다. 아래 표는 실제 컨트롤러 기준으로 최신화되어 있으니,
> 신규 엔드포인트 추가 시 이 표의 URL/Method 컨벤션을 따르세요(불일치 발견 시 실제 컨트롤러 코드를 우선 신뢰).

---

## 1. USER (user-service, 담당: 동원)

| 기능                    | URL                                      | Method | 비고                                                                                           |
|-----------------------|------------------------------------------|--------|----------------------------------------------------------------------------------------------|
| 회원가입                  | `/api/v1/auth/signup`                    | POST   |                                                                                              |
| 로그인                   | `/api/v1/auth/login`                     | POST   | OAuth 전용(비밀번호 미설정) 유저는 로그인 불가. LOGIN_FAILED 반환                                               |
| 로그아웃                  | `/api/v1/auth/logout`                    | POST   |                                                                                              |
| 토큰 재발급                | `/api/v1/auth/refresh`                   | POST   |                                                                                              |
| 소셜 로그인 URL 생성         | `/api/v1/auth/social/login-url`          | POST   | Body: provider, codeChallenge, state — 프론트가 생성한 PKCE 값을 받아 OAuth URL 반환                      |
| 소셜 로그인 (Google/Kakao) | `/api/v1/auth/social/login`              | POST   | Body: provider, code, codeVerifier — Authorization Code + PKCE 흐름, 서비스 JWT 발급                |
| 내 정보 조회               | `/api/v1/users/me`                       | GET    | Response에 `profile`(이모지 or S3 URL), `oauthProvider`, `integrated` 포함                         |
| 내 정보 수정               | `/api/v1/users/me`                       | PATCH  | name, nickname, phone만 수정 가능. 비밀번호·프로필 변경은 별도 엔드포인트 사용                                       |
| 비밀번호 변경               | `/api/v1/users/me/password`              | POST   | Body: currentPassword, newPassword. 현재 비밀번호 확인 후 변경                                          |
| 회원 탈퇴                 | `/api/v1/users/me`                       | DELETE | Body(optional): `{ "password": "..." }`. 비밀번호 설정 유저는 필수, OAuth 전용 유저는 생략 가능                  |
| 통합 회원 전환              | `/api/v1/users/me/integrate`             | POST   | OAuth 가입 유저 전용. Body: `{ "password": "..." }`. 비밀번호 설정 + integrated=true. 이후 이메일/비밀번호 로그인 가능 |
| 프로필 Presigned URL 발급  | `/api/v1/users/me/profile/presigned-url` | GET    | Query: `extension` (jpg/jpeg/png/webp). Response: `{ presignedUrl, s3Url }`. 유효시간 3분         |
| 프로필 수정                | `/api/v1/users/me/profile`               | PATCH  | Body: `{ "profile": "😀" }` 또는 `{ "profile": "https://cdn.moni.my/..." }`. 이모지·CDN URL 모두 허용 |
| 투자 성향 등록              | `/api/v1/users/me/tendency`              | POST   |                                                                                              |
| 투자 성향 조회              | `/api/v1/users/me/tendency`              | GET    |                                                                                              |
| 투자 성향 수정              | `/api/v1/users/me/tendency`              | PUT    |                                                                                              |
| 관심사 등록                | `/api/v1/users/me/interests`             | POST   |                                                                                              |
| 관심사 조회                | `/api/v1/users/me/interests`             | GET    |                                                                                              |
| 관심사 수정                | `/api/v1/users/me/interests`             | PUT    |                                                                                              |
| 관심종목 추가               | `/api/v1/users/me/watchlist/{stockCode}` | PUT    | stockCode: 영문/숫자 6자리 (`^[A-Za-z0-9]{6}$`)                                                    |
| 관심종목 삭제               | `/api/v1/users/me/watchlist/{stockCode}` | DELETE | stockCode: 영문/숫자 6자리                                                                         |
| 관심종목 목록 조회            | `/api/v1/users/me/watchlist`             | GET    |                                                                                              |

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

## 2. 모의 투자 서비스 (trade-service, 담당: 동민/설아)

| 기능              | URL                                 | Method | 비고               |
|-----------------|-------------------------------------|--------|------------------|
| 주식 매수           | `/api/v1/trades/buy`                | POST   |                  |
| 주식 매도           | `/api/v1/trades/sell`               | POST   |                  |
| 거래 내역 조회        | `/api/v1/trades`                    | GET    | Query: page, size (기본 0/10) |
| 보유 종목 조회        | `/api/v1/holdings`                  | GET    |                  |
| 보유 종목 현황 조회     | `/api/v1/holdings/{ticker}`         | GET    |                  |
| 가상 계좌 생성        | `/api/v1/accounts`                  | POST   |                  |
| 내 가상 자산 잔액 조회   | `/api/v1/accounts/me`               | GET    |                  |
| 보유 종목 현황 조회     | `/api/v1/assets/holdings`           | GET    | 담당: 설아           |
| 자산 조회           | `/api/v1/assets`                    | GET    | 담당: 설아           |
| AI 분석용 자산 스냅샷 조회 | `/api/v1/assets/analysis-snapshot`  | GET    | 담당: 설아, AI 포트폴리오 분석용 자산 요약 + 보유 비중 상위 종목 |
| 예약 매수 주문 등록     | `/api/v1/reserved-orders/buy`       | POST   |                  |
| 예약 매도 주문 등록     | `/api/v1/reserved-orders/sell`      | POST   |                  |
| 예약 주문 취소        | `/api/v1/reserved-orders/{orderId}` | DELETE |                  |
| 내 예약 주문 목록 조회   | `/api/v1/reserved-orders`           | GET    |                  |

---

## 3. 투자 종목 조회 / 시세 조회 (stock-service, 담당: 영욱)

| 기능                 | URL                             | Method | 비고                                   |
|--------------------|---------------------------------|--------|--------------------------------------|
| 종목 검색 조회           | `/api/v1/stocks/search`         | GET    |                                      |
| 종목 목록 조회           | `/api/v1/stocks/search`         | GET    | 검색과 동일 URL로 기재됨 — 파라미터로 구분할지 사용자와 확인 |
| 단일 종목 상세 조회        | `/api/v1/stocks/{ticker}`       | GET    |                                      |
| 차트 조회              | `/api/v1/stocks/{ticker}/chart` | GET    |                                      |
| 실시간 인기 테마 조회       | `/api/v1/stocks/themes`         | GET    |                                      |
| 실시간 거래량 상위 Top5 조회 | `/api/v1/stocks/top-volume`     | GET    |                                      |

---

## 4. 포트폴리오 (portfolio-service, 담당: 설아)

| 기능                | URL                                                     | Method | 비고                                                                            |
|-------------------|---------------------------------------------------------|--------|-------------------------------------------------------------------------------|
| 포트폴리오 생성          | `/api/v1/portfolio`                                     | POST   |                                                                               |
| ~~수익률 계산~~        | ~~`/api/v1/portfolio/returns`~~                         | ~~GET~~  | 현재 누적 수익률은 **자산 조회**에서 제공 (필요 시 복구)                                    |
| ~~종목별 손익~~        | ~~`/api/v1/portfolio/holdings/{stockCode}/profit-loss`~~ | ~~GET~~    | **보유 종목 현황**에서 평가손익·수익률을 제공 (필요 시 복구)                               |
| AI 포트폴리오 분석 요청    | `/api/v1/portfolio/ai-analysis`                         | POST   | 내부적으로 ai-service의 `/api/v1/ai/portfolio/analysis` 호출(Feign, Authorization 헤더 전달) |
| AI 포트폴리오 분석 최신 조회 | `/api/v1/portfolio/ai-analysis/latest`                  | GET    |                                                                               |
| AI 포트폴리오 분석 단건 조회 | `/api/v1/portfolio/ai-analysis/{analysisId}`            | GET    |                                                                               |
| AI 포트폴리오 분석 전체 조회 | `/api/v1/portfolio/ai-analysis?page=&size=`                   | GET    |                                                                         |

---

## 5. AI 서비스 (ai-service, 담당: 지은/설아)

| 기능             | URL                                  | Method | 비고                                                             |
|----------------|--------------------------------------|--------|----------------------------------------------------------------|
| 기업 이슈 분석       | `/api/v1/ai/{ticker}/issue-analysis` | POST   |                                                                |
| 기업 이슈 분석 조회    | `/api/v1/ai/{ticker}/issue-analysis` | GET    |                                                                |
| 뉴스 요약 조회       | `/api/v1/ai/news-summary`            | POST   |                                                                |
| 포트폴리오 AI 분석    | `/api/v1/ai/portfolio/analysis`      | POST   | header: `Authorization` 필요. portfolio-service가 호출하는 내부 API / 담당: 설아 |
| 뉴스 fetch       | `/api/v1/ai/admin/news/fetch`        | POST   | 스케줄러 또는 매니저용.                                                  |
| AI 분석 가능 기업 조회 | `/api/v1/ai`                         | GET    |                                                                |
| News 직접 등록     | `/api/v1/admin/ai/news/ticker`       | POST   |                                                                |
| 거시 시장 뉴스 fetch | `/api/v1/admin/ai/news/market/fetch` | POST   | 스케줄러 또는 매니저용.                                                  |
| 뉴스 목록 조회       | `/api/v1/ai/news` | GET    |                                                                |




---

## 6. 결제 (payment-service, 담당: 혜수)

| 기능           | URL                                     | Method | 비고                              |
|--------------|------------------------------------------|--------|---------------------------------|
| Toss 정기 구독 결제 | `/api/v1/payments/subscription`         | POST   | Header: `X-User-Id`. Toss billing 연동 |
| 결제 내역 조회     | `/api/v1/payments`                      | GET    | Header: `X-User-Id`. Pageable (기본 size=10) |
| 구독 상태 조회     | `/api/v1/payments/subscriptions/status` | GET    | Header: `X-User-Id`             |
| 구독 해지        | `/api/v1/payments/subscriptions`        | DELETE | Header: `X-User-Id`             |
| 구독 재활성화      | `/api/v1/payments/subscriptions/reactivate` | POST | Header: `X-User-Id`. SUSPENDED 상태만 가능 |

---

## 7. 알림 (notification-service, 담당: 혜수)

원본 노션 문서에 별도 API 표가 작성되지 않았습니다. P1 "사용자 맞춤 알림 서비스(ex. 미장 개장 10분 전)"
구현 시 API 설계가 필요하면 사용자에게 요청 패턴(폴링 vs 웹소켓/SSE vs FCM push 등)을 먼저 확인하세요.
