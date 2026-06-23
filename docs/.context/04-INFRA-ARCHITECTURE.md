# 04. 인프라 설계 / 비기능 요구사항 / CI·CD / 모니터링

---

## 1. 인프라 구성 (Layer / Subnet)

> 원본 기획서의 AWS 배치 설계. `mysql → postgredb`로 변경됨(모든 DB는 PostgreSQL).

| Layer             | 요소                                  | 서브넷          |
|-------------------|-------------------------------------|--------------|
| Ingress           | NGINX → Gateway                     | public       |
| Service Discovery | Eureka                              | public       |
| Application       | MSA 컨테이너 6개 (도메인 서비스)               | private      |
| Data              | Kafka Cluster, Redis, PostgreSQL 7개 | private data |

> ⚠️ 현재 MVP 단계에서는 **k8s를 사용하지 않기로 결정**됨 (`docs/06-AI-VALIDATION-LOG.md` 참고).
> 단, 저장소에는 `k8s/` 디렉토리(각 서비스별 deployment/service manifest)가 이미 존재합니다.
> 이는 추후 전환을 위한 준비물이거나 과거 산출물일 수 있으니, k8s 관련 작업을 요청받으면
> 사용자에게 현재도 유효한지 먼저 확인하세요. **MVP 단계에서는 docker-compose 기반 운영이
> 기본값**입니다.

---

## 2. 비기능적 요구사항(NFR)

| 속성              | 목표                                             | 적용 가능한 기술(논의 필요)              |
|-----------------|------------------------------------------------|-------------------------------|
| Scalability     | 수평 확장 대응                                       | MSA + Eureka 로드밸런싱            |
| Availability    | 단일 서비스 장애가 전파되지 않도록                            | Circuit Breaker, Kafka 비동기 처리 |
| Performance     | API 평균 응답 시간 200ms 이하, 반복 조회 데이터 캐시 적중률 80% 이상 | Redis Cache-Aside 전략          |
| Throughput      | 주문/이벤트 등 비동기 처리                                | Kafka Topic Partitioning      |
| Consistency     | 주문 - 재고 - 배송(또는 거래-계좌-포트폴리오) 간 데이터 정합성         | Kafka + Saga 패턴               |
| Maintainability | 단일 서비스 변경이 타 서비스에 영향이 없도록                      | DDD 기반 서비스 경계 분리              |

---

## 3. 기술 스택 / 시스템 구조

| 항목                | 내용                                                                            |
|-------------------|-------------------------------------------------------------------------------|
| 언어/프레임워크          | Java 21 / Spring Boot 3.5.0                                                   |
| Build             | Gradle (멀티모듈)                                                                 |
| 서비스 간 동기 통신       | FeignClient (REST)                                                            |
| API Gateway       | Spring Cloud Gateway                                                          |
| Service Discovery | Spring Cloud Eureka                                                           |
| Config            | Spring Cloud Config Server (`config-server/src/main/resources/configs/*.yml`) |
| DB                | PostgreSQL (서비스별 분리)                                                          |
| 인증                | OAuth + JWT (사용자), Okta OIDC (관리자)                                            |
| 인프라               | Docker / Docker Compose                                                       |
| 비동기 메시징           | Kafka                                                                         |
| 캐시                | Redis                                                                         |
| AI 외부 API         | OpenAI                                                                        |
| 분산 추적             | Zipkin                                                                        |
| 문서화 / 테스트         | Swagger(springdoc-openapi) / Postman & JUnit 5                                |
| Spring Cloud 버전   | 2025.0.0                                                                      |

---

## 4. 서비스 / 인프라 포트맵 (현재 docker-compose 기준)

### 애플리케이션 서비스

| 서비스                  | 포트    | 설명                                   |
|----------------------|-------|--------------------------------------|
| config-server        | 8888  | 설정 서버                                |
| eureka-server        | 8761  | 서비스 디스커버리                            |
| api-gateway          | 8080  | API 게이트웨이                            |
| user-service         | 19090 | 회원/인증                                |
| trade-service        | 19091 | 매수/매도/체결                             |
| stock-service        | 19092 | 실시간 시세/종목 조회                         |
| portfolio-service    | 19093 | 포트폴리오                                |
| notification-service | 19094 | 알림                                   |
| payment-service      | 19095 | 구독/결제                                |
| ai-service           | 19096 | AI 분석/뉴스 요약                          |
| admin-service        | 19097 | 관리자 웹 UI (Okta OIDC, api-gateway 우회) |

### 인프라

| 인프라             | 포트(호스트) | 설명                    |
|-----------------|---------|-----------------------|
| user-db         | 25432   | 회원 DB (PostgreSQL)    |
| portfolio-db    | 25433   | 포트폴리오 DB (PostgreSQL) |
| stock-db        | 25434   | 시세 DB (PostgreSQL)    |
| notification-db | 25435   | 알림 DB (PostgreSQL)    |
| trade-db        | 25436   | 거래 DB (PostgreSQL)    |
| payment-db      | 25437   | 결제 DB (PostgreSQL)    |
| ai-db           | 25438   | AI DB (PostgreSQL)    |
| redis           | 26379   | 캐시 / 세션               |
| zookeeper       | 22181   | Kafka 코디네이션           |
| kafka           | 29092   | 메시지 브로커               |

> `docker-compose.infra.yml`에서 DB 계정/비밀번호는 `.env`(`.env.example` 참고)의 환경변수로
> 주입됩니다. 새 인프라 컴포넌트를 추가할 때도 동일한 패턴(서비스별 컨테이너 + named volume +
> `.env` 변수)을 따르세요.

> `docker-compose.monitor.yml`은 **현재 전체가 주석 처리**되어 있습니다 (Prometheus/Grafana
> 정의는 있으나 비활성 상태). 모니터링 작업을 시작할 때는 이 파일의 주석을 해제하고
> `monitoring/prometheus.yml`을 함께 구성해야 합니다.

---

## 5. CI/CD 파이프라인

### Dev-CI flow (`develop` 브랜치, PR 생성 시 실행)

개발용 통합 브랜치. 기능 개발 후 통합 전, 코드 정적 분석 및 단위 테스트 통과 여부를 빠르게
확인해 코드 퀄리티를 유지하는 것이 목표.

1. **Lint Check** — 코드 컨벤션 준수 여부 확인 (Checkstyle)
2. **Build** — 빌드 시 테스트를 통해 빌드 성공 여부 확인
3. **Unit Test & Coverage** — 비즈니스 로직 무결성 검증. **커버리지 80% 미달 시 배포 차단**
4. 성공 시 수동 머지. 실패 시 PR에 실패 코멘트 등록

### Main-CI flow (`main` 브랜치, PR 생성 시 실행)

배포용 브랜치. 기능 개발 후 배포 전, 정적 분석/단위 테스트/통합 테스트까지 빠르게 확인.

1. **Lint Check** — 코드 컨벤션 준수 여부 확인
2. **Build** — 빌드 시 테스트를 통해 빌드 성공 여부 확인
3. **Containerization** — Docker 이미지화 후 AWS ECR에 관리
4. **Unit Test & Coverage** — 커버리지 80% 미달 시 배포 차단
5. **Integration Test** — `docker-compose`로 DB 등 외부 의존성 연결 확인
6. 실패 시 이미지가 이전 tag로 rollback + PR에 실패 코멘트. 성공 시 CD 파이프라인 실행

> CI 워크플로 실제 정의는 `.github/workflows/`에 있습니다. 새 워크플로/스텝을 추가/수정할 때는
> 위 흐름과 일치하는지 확인하세요.

---

## 6. 모니터링 및 로깅 전략

- Trace ID 전파 (Zipkin 분산 추적)
- 로그 레벨 관리
- 필요한 정보 로깅: `trace id / user id / 도메인 정보 / 에러 정보 / etc.`
- 민감 정보 마스킹 (예: 이메일, 카드 정보, 토큰 등은 로그에 그대로 남기지 않기)

### Prometheus + Grafana (필수 요구사항)

- Prometheus로 서비스 메트릭(CPU, 메모리, 요청 처리 속도 등) 수집
- Grafana로 시각화 대시보드 구성
- ELK 사용 조는 Kibana + Metricbeat로 대체 가능

### Slack 알림 (필수 요구사항)

- Grafana Alerting: **CPU 사용량 50% 이상**일 때 Slack Webhook으로 알림 전송
- 알림 채널/조건은 Grafana Alerting 룰로 정의

### (도전) AIOps 파이프라인

기본 모니터링(Prometheus+Grafana+Slack)을 완료한 뒤에만 진행:

1. Prometheus → 메트릭 수집
2. Alertmanager → 조건 충족 시 Alert 생성
3. Webhook 서버(중간 서버) → Alert 데이터 수신
4. LLM API 호출 → Alert + Metrics + Logs + Traces 종합 분석
5. Slack 전송 → 사람이 읽을 수 있는 장애 대응 리포트(액션 아이템 포함)

---

## 7. 부하 테스트 (JMeter + Kafka 비동기 처리) — 필수 요구사항

- 대상: 가장 트래픽이 몰릴 것 같은 API 1개 이상 (예: 주식 매수/매도 `/api/v1/trades/buy|sell`,
  또는 시세 조회 API)
- JMeter 설정: **Thread 100 / Ramp-up 1초 / Loop Count 10** (총 1,000 요청)
- 측정 지표: 요청당 평균 응답시간(Average), 처리량(Throughput), 오류율(Error %)
- 1차 목표: 해당 환경에서 **Throughput 200/sec 이상** (도달 못해도 개선 전/후 비교 제출 시 인정)
- **필수**: 응답 반환 후 Kafka에 요청 데이터를 적재하고, 별도 Consumer가 순차 처리 + 처리 상태 로그 기록
  (= "동기 응답 → 비동기 후처리" 패턴)
- Kafka 파티션 수와 키 선택 근거를 1줄로 기록 (예: "파티션 3개, key=tradeId — 거래 단위 순서 보장 + 3배 병렬")
- 측정 환경(로컬/Docker/클라우드, CPU/메모리, DB·Kafka 위치)을 결과와 함께 반드시 기록

개선 사이클 흐름: 부하 테스트 → 병목 발견(느린 평균 응답, DB 커넥션 풀 부족, 반복 조회, 외부 호출
지연, 과다 로깅 등) → 개선(Kafka 비동기화, Redis 캐싱, HikariCP 튜닝, 인덱스, Feign timeout +
Resilience4j) → 재측정. 전/후 비교표를 산출물로 남길 것.

---

## 8. RAG 기반 AI 고객 응대 (도전 과제, ai-service)

- 권장 스택: 벡터DB = pgvector(이미 PostgreSQL 사용 중이므로 별도 인프라 불필요) 또는 Chroma(로컬)
- 임베딩/LLM: OpenAI 또는 무료 티어 제공자, 비용 부담 시 로컬 임베딩(Sentence-Transformers) 대체 가능
- Spring AI의 RAG 파이프라인(VectorStore + ChatClient) 사용
- 흐름: 도메인 문서 청킹/벡터화 → 사용자 질문 → 벡터 검색 → 관련 문서 추출 → LLM 응답 생성(출처 포함)
- MVP 통과 기준: 문서 1종 벡터화 → 질문 1건에 대해 참조 출처 포함 응답 생성
- ai-service의 `news`/`news_embedding`/`news_summary`/`company_issue_analysis` 테이블
  (`03-ERD.md` 4번 섹션)이 이 파이프라인의 데이터 모델 기반이 됩니다.
