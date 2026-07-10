# CI/CD 흐름 및 트러블슈팅

## 전체 흐름

```
feature/* → PR to stage
              ↓
           CI (build job)
           - stage PR 1개 초과 시 즉시 실패 + Discord 알림
           - Gradle 빌드 + 테스트
           - 변경된 서비스 감지 (git diff 기반)
           - JAR artifact 저장 (1일 보관)
              ↓ (CI 성공 후 workflow_call, 변경 서비스 있을 때만)
           CD (deploy job) — stage PR에서만 실행
           - JAR artifact 재사용 (Gradle 빌드 생략)
           - 변경된 서비스만 Docker 이미지 빌드 + ECR 푸시 (5개씩 병렬)
           - Bastion ProxyJump → Service EC2 SSH 배포
           - 헬스체크 (15초 간격 × 최대 12회)
           - 실패 시 이전 이미지로 자동 롤백
              ↓
           배포 확인 후 수동 머지
```

---

## 트리거 조건

| 이벤트                         | CI | CD           |
|-----------------------------|----|--------------|
| push (master/develop/stage) | ✅  | ❌            |
| PR to master/develop        | ✅  | ❌            |
| PR to stage                 | ✅  | ✅ (CI 성공 후)  |
| workflow_dispatch           | ❌  | ✅ (수동 실행 가능) |

> CD는 **stage 대상 PR**에서만 자동 트리거됩니다.  
> stage에 머지된 후 push로 발생하는 CI에서는 CD가 실행되지 않습니다.

---

## stage 브랜치 PR 제한

stage 브랜치에 동시에 PR이 2개 이상 열리면 CI가 즉시 실패합니다.  
검증 중인 코드가 뒤섞여 배포되는 것을 방지합니다.  
기존 stage PR이 머지 또는 종료된 후 다시 실행합니다.

---

## 머지 전 배포를 선택한 이유

stage에 머지한 뒤 CD를 실행하면, CD 중 문제가 생겼을 때 이미 stage에 합쳐진 상태라 해당 브랜치에서 바로 재작업이 어렵습니다.  
PR 단계에서 CI 통과 후 배포하면 문제 발생 시 해당 브랜치에서 바로 수정 후 재시도할 수 있습니다.  
머지는 배포까지 정상 확인된 뒤 진행하는 것이 목표입니다.

---

## Secrets 목록

| 이름                      | 설명                                             |
|-------------------------|------------------------------------------------|
| `AWS_ACCESS_KEY_ID`     | ECR 이미지 push용 IAM 액세스 키                        |
| `AWS_SECRET_ACCESS_KEY` | ECR 이미지 push용 IAM 시크릿 키                        |
| `ECR_REGISTRY`          | ECR 레지스트리 주소                                   |
| `BASTION_HOST`          | Bastion EC2 Public IP                          |
| `BASTION_HOST_KEY`      | Bastion SSH host key (known_hosts 등록용)         |
| `SERVICE_HOST`          | Service EC2 Private IP                         |
| `SERVICE_HOST_KEY`      | Service EC2 SSH host key (known_hosts 등록용)     |
| `SSH_KEY`               | Service EC2 접속용 PEM (Bastion을 통한 ProxyJump 사용) |
| `DISCORD_WEBHOOK_URL`   | Discord 알림 웹훅 URL                              |
| `HEALTH_CHECK_PORTS`    | 헬스체크 포트 매핑 (`서비스=포트` 공백 구분)                    |

> `BASTION_HOST_KEY`, `SERVICE_HOST_KEY`는 `ssh-keyscan -t ed25519 -H <IP>` 결과를 그대로 저장합니다.

---

## JAR Artifact

- CI 실행마다 새로운 JAR artifact를 생성합니다 (`retention-days: 1`)
- CD는 동일 Workflow Run의 artifact를 재사용해 Gradle 빌드 중복 실행을 방지합니다
- `workflow_dispatch` 수동 실행 시에는 artifact가 없으므로 CD 내부에서 직접 빌드합니다

---

## Docker 병렬 빌드

- Docker 이미지는 최대 5개씩 병렬 빌드하여 전체 배포 시간을 단축합니다
- 각 빌드 PID를 추적해 하나라도 실패하면 전체 스텝을 실패 처리합니다

---

## 서비스 변경 감지

CI에서 `git diff origin/stage...HEAD`로 변경된 파일을 분석해 배포 대상 서비스를 결정합니다.  
변경이 없는 서비스는 빌드와 배포 모두 건너뜁니다.

**전체 빌드가 트리거되는 경우**

아래 경로에 변경이 있으면 모든 서비스를 빌드·배포합니다.

- `common/`, `common-logging/` — 공통 모듈 변경
- 루트 `build.gradle`, `settings.gradle`, `gradle/` — 빌드 설정 변경
- `.github/` — 워크플로우 변경 시 실제 배포 환경에서 검증

**서비스 개별 감지**

`<서비스명>/` 하위 파일이 변경된 서비스만 대상에 포함합니다.

**config-server 설정 파일 연동**

`config-server/src/main/resources/configs/<서비스명>.yml`이 변경되면 해당 서비스도 재시작 대상에 포함합니다.  
config-server만 재시작해서는 실행 중인 서비스가 새 설정을 적용받지 못하기 때문입니다.

변경된 서비스가 없으면 CD 자체가 실행되지 않습니다.

---

## 헬스체크

배포 후 변경된 서비스의 `/actuator/health` 엔드포인트를 확인합니다.  
api-gateway를 거치지 않고 서비스 EC2 내부에서 `localhost:PORT`로 직접 요청합니다.

- Docker 컨테이너 내부에서 curl을 실행합니다 (`docker exec moni-<서비스>-1 curl ...`)
- 15초 간격으로 최대 12회 재시도 (최대 3분 대기)
- 모든 서비스가 동시에 시작되므로 실질적인 대기 시간은 가장 늦게 뜨는 서비스 1개의 시작 시간과 같습니다
- 재시도 안에 응답하지 않으면 해당 서비스를 실패로 처리하고 롤백을 진행합니다

---

## 자동 롤백

헬스체크 실패 시 이전 배포 이미지로 자동 복원합니다.

1. 배포 전 현재 실행 중인 SHA를 서버의 `~/moni/.deployed-sha`에 저장합니다
2. 새 이미지로 배포한 뒤 헬스체크를 수행합니다
3. 헬스체크가 실패하면 저장해둔 이전 SHA로 ECR에서 이미지를 pull해 재시작합니다
4. 롤백 완료 후 `.deployed-sha`를 이전 SHA로 복원합니다

ECR에 이미지가 최대 2개 보관되므로 직전 배포까지만 롤백이 가능합니다.  
이전 SHA 이미지가 ECR에 없으면 롤백에 실패하고 수동 대응이 필요합니다.