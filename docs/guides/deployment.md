# CI/CD 흐름 및 트러블슈팅

## 전체 흐름

```
feature/* → PR to stage
              ↓
           CI (build job)
           - stage PR 1개 초과 시 즉시 실패 + Discord 알림
           - Gradle 빌드 + 테스트
           - JAR artifact 저장 (1일 보관)
              ↓ (CI 성공 후 workflow_call)
           CD (deploy job) — stage PR에서만 실행
           - JAR artifact 재사용 (Gradle 빌드 생략)
           - Docker 이미지 빌드 + ECR 푸시 (5개씩 병렬)
           - Bastion ProxyJump → Service EC2 SSH 배포
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