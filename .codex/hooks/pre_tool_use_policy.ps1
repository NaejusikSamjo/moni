# MONI Codex PreToolUse policy hook.
#
# 적용 위치:
# - C:\spring\moni\.codex\hooks\pre_tool_use_policy.ps1
#
# 목적:
# - Codex가 shell 명령을 실행하기 전에 위험 명령을 차단합니다.
# - 입력 JSON 구조는 Codex 버전/표면에 따라 달라질 수 있으므로,
#   우선 stdin 전체 문자열에서 command 패턴을 보수적으로 검사합니다.
#
# 주의:
# - 이 hook은 안전장치입니다. 운영 credential, 실제 secret은 repo에 두지 않는 것이 원칙입니다.
# - 차단 규칙을 너무 넓게 잡으면 정상적인 빌드/테스트도 막을 수 있습니다.

$ErrorActionPreference = "Stop"

$rawInput = [Console]::In.ReadToEnd()

function Deny-Command {
    param(
        [string] $Reason
    )

    Write-Error "[MONI Codex Policy] $Reason"
    exit 1
}

# 입력이 비어 있거나 shell 명령이 아닌 도구 호출이면 통과시킵니다.
if ([string]::IsNullOrWhiteSpace($rawInput)) {
    exit 0
}

# Windows/PowerShell 경로 구분과 대소문자 차이를 고려해 소문자로 정규화합니다.
$normalized = $rawInput.ToLowerInvariant()

# 1. Git 이력 파괴/강제 push 방지
if ($normalized -match "git\s+reset\s+--hard") {
    Deny-Command "git reset --hard 는 사용자 변경사항을 삭제할 수 있어 금지합니다."
}

if ($normalized -match "git\s+clean\s+-[^\s]*f") {
    Deny-Command "git clean -f 계열 명령은 추적되지 않은 파일을 삭제할 수 있어 금지합니다."
}

if ($normalized -match "git\s+push[^\r\n]*--force") {
    Deny-Command "git push --force 는 원격 이력을 덮어쓸 수 있어 금지합니다."
}

# 2. Docker 볼륨/데이터 삭제 방지
if ($normalized -match "docker\s+compose[^\r\n]*down[^\r\n]*-v") {
    Deny-Command "docker compose down -v 는 DB/Redis/Kafka 볼륨을 삭제할 수 있어 금지합니다."
}

if ($normalized -match "docker\s+volume\s+rm") {
    Deny-Command "docker volume rm 은 영구 데이터를 삭제할 수 있어 금지합니다."
}

if ($normalized -match "docker\s+system\s+prune[^\r\n]*--volumes") {
    Deny-Command "docker system prune --volumes 는 볼륨 데이터를 삭제할 수 있어 금지합니다."
}

# 3. 운영/배포 대상에 대한 부하 테스트 방지
if (
    ($normalized -match "k6\s+run" -or $normalized -match "docker\s+run[^\r\n]*k6") -and
    (
        $normalized -match "www\.moni\.my" -or
        $normalized -match "api\.moni\.my" -or
        $normalized -match "prod" -or
        $normalized -match "production"
    )
) {
    Deny-Command "운영/배포 주소를 대상으로 한 k6 부하 테스트는 금지합니다. 로컬 또는 테스트 환경만 사용하세요."
}

# 4. 민감 파일 직접 출력 방지
if (
    ($normalized -match "get-content" -or $normalized -match "\btype\b" -or $normalized -match "\bcat\b") -and
    (
        $normalized -match "\.env" -or
        $normalized -match "application-prod" -or
        $normalized -match "application-secret" -or
        $normalized -match "secret" -or
        $normalized -match "credential"
    )
) {
    Deny-Command "민감 설정 파일을 직접 출력하는 명령은 금지합니다."
}

# 5. 광범위한 삭제 명령 방지
if ($normalized -match "remove-item[^\r\n]*-recurse[^\r\n]*-force") {
    Deny-Command "Remove-Item -Recurse -Force 는 광범위한 삭제가 가능해 금지합니다. 필요한 경우 사용자가 직접 승인해야 합니다."
}

exit 0
