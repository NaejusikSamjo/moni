## 서비스 구성

| 서비스                  | 포트    | 설명           |
|----------------------|-------|--------------|
| config-server        | 8888  | 설정 서버        |
| eureka-server        | 8761  | 서비스 디스커버리    |
| api-gateway          | 8080  | API 게이트웨이    |
| user-service         | 19090 | 회원/인증        |
| trade-service        | 19091 | 매수/매도/체결     |
| stock-service        | 19092 | 실시간 시세/종목 조회 |
| portfolio-service    | 19093 | 포트폴리오        |
| notification-service | 19094 | 알림           |
| payment-service      | 19095 | 구독/결제        |
| ai-service           | 19096 | AI 분석/뉴스 요약  |

## 인프라 구성

| 인프라             | 포트    | 설명                    |
|-----------------|-------|-----------------------|
| user-db         | 25432 | 회원 DB (PostgreSQL)    |
| portfolio-db    | 25433 | 포트폴리오 DB (PostgreSQL) |
| stock-db        | 25434 | 시세 DB (PostgreSQL)    |
| notification-db | 25435 | 알림 DB (PostgreSQL)    |
| trade-db        | 25436 | 거래 DB (PostgreSQL)    |
| payment-db      | 25437 | 결제 DB (PostgreSQL)    |
| ai-db           | 25438 | AI DB (PostgreSQL)    |
| redis           | 26379 | 캐시 / 세션               |
| zookeeper       | 22181 | Kafka 코디네이션           |
| kafka           | 29092 | 메시지 브로커               |

## 기술 스택

- Java 21
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0
- Docker / Docker Compose
- Kafka
- Redis

## 실행 방법

### 1. 인프라 실행

```bash
docker compose -f docker-compose.infra.yml up -d
```

### 2. 애플리케이션 실행

```bash
docker compose -f docker-compose.yml up -d --build
```
