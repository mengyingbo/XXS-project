# =============================================================
# 通用单关题库实测脚本（阶段 3）
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\question-level-verify.ps1 `
#     -LevelId 13 -AnswersJson '["0","0","true","2","豆荚"]'
# 流程：注册临时孩子 -> SQL 补齐目标关之前所有关卡的通关进度 ->
#       PIN 登录 -> 目标关应为 UNLOCKED -> 开局 -> 按给定答案提交 ->
#       校验全对/三星/积分(5题关=105, 4题关=80) -> 输出临时孩子 id
# 清理：question-level-cleanup.sql（脚本自动生成并执行）
# =============================================================
param(
    [Parameter(Mandatory = $true)][int]$LevelId,
    [Parameter(Mandatory = $true)][string]$AnswersJson,
    [int]$QuestionCount = 5,
    [int]$ExpectedPoints = 105
)

$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080/api'
$script:pass = 0
$script:fail = 0

function Call {
    param([string]$Method, [string]$Path, $Body, [string]$Token)
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $params = @{ Uri = "$base$Path"; Method = $Method; UseBasicParsing = $true; Headers = $headers }
    if ($null -ne $Body) {
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 12 -Compress }
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes($json)
        $params['ContentType'] = 'application/json; charset=utf-8'
    }
    try {
        $resp = Invoke-WebRequest @params
        $stream = $resp.RawContentStream
        $stream.Position = 0
        $text = [System.Text.Encoding]::UTF8.GetString($stream.ToArray())
        return $text | ConvertFrom-Json
    } catch {
        return [pscustomobject]@{ code = -1; message = $_.Exception.Message; data = $null }
    }
}

function Check {
    param([string]$Name, $Condition, $Detail)
    if ($Condition) {
        $script:pass++
        Write-Host ("[PASS] " + $Name) -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host ("[FAIL] " + $Name + "  >>> " + $Detail) -ForegroundColor Red
    }
}

# 期望答案（按题目顺序）
$expected = $AnswersJson | ConvertFrom-Json

Write-Host ("===== 实测目标：levelId=" + $LevelId + "，题数=" + $QuestionCount + " =====") -ForegroundColor Cyan
$health = Call GET '/health'
Check '服务健康 + DB 连通' ($health.code -eq 0 -and $health.data.db.connected) $health.message

$reg = Call POST '/child/register' @{ nickname = 'ApiTest'; avatar = 'cat'; pin = '1234' }
Check '新建临时孩子档案' ($reg.code -eq 0) $reg.message
$childId = $reg.data.id

# SQL 补齐目标关之前所有关卡的 PASSED 进度（仅用于解锁目标关）
$seedSql = "INSERT INTO progress (child_id, level_id, status, stars, best_accuracy, best_score, attempt_count, first_passed_at) SELECT $childId, id, 'PASSED', 3, 100.00, 105, 1, NOW() FROM level WHERE id < $LevelId;"
$seedFile = 'C:\Users\admin\AppData\Local\Temp\xxs_seed_progress.sql'
[System.IO.File]::WriteAllText($seedFile, $seedSql, (New-Object System.Text.UTF8Encoding -ArgumentList $false))

$env:SSH_ASKPASS = 'C:\Users\admin\AppData\Local\Temp\xa.bat'
$env:SSH_ASKPASS_REQUIRE = 'force'
$env:DISPLAY = 'localhost:0'
scp -o StrictHostKeyChecking=no $seedFile mengyingbo@192.168.1.53:/tmp/xxs_seed_progress.sql 2>$null
$seedOut = ssh -o StrictHostKeyChecking=no mengyingbo@192.168.1.53 "mysql -uxxs -pXxs2026pwd --default-character-set=utf8mb4 -N -B xxs_game < /tmp/xxs_seed_progress.sql 2>&1"
Check '已补齐前置关卡通关进度' ($LASTEXITCODE -eq 0) $seedOut

$cLogin = Call POST '/child/login' @{ childId = $childId; pin = '1234' }
Check 'PIN 正确登录' ($cLogin.code -eq 0) $cLogin.message
$child = $cLogin.data.token

# 目标关在地图中的状态
$map = Call GET '/child/map' $null $child
$targetNode = $null
foreach ($u in $map.data.units) {
    foreach ($l in $u.lessons) {
        foreach ($lv in $l.levels) {
            if ($lv.id -eq $LevelId) { $targetNode = $lv }
        }
    }
}
Check '地图中找到目标关' ($null -ne $targetNode) ('levelId=' + $LevelId)
Check '目标关状态为 UNLOCKED' ($targetNode.status -eq 'UNLOCKED') ($targetNode.status)

$start = Call POST '/child/session/start' @{ levelId = $LevelId } $child
Check ("开局返回 " + $QuestionCount + " 道题") ($start.code -eq 0 -and $start.data.questions.Count -eq $QuestionCount) ($start.data | ConvertTo-Json -Compress)

# 构造作答
$answers = @()
for ($i = 0; $i -lt $QuestionCount; $i++) {
    $answers += [pscustomobject]@{
        questionId = $start.data.questions[$i].id
        userAnswer = [string]$expected[$i]
        durationMs = 1000
    }
}
$submit = Call POST '/child/session/submit' @{ levelId = $LevelId; durationMs = ($QuestionCount * 1000); answers = $answers } $child
Check '提交成功' ($submit.code -eq 0) $submit.message
Check ("全对（correctCount=" + $QuestionCount + "）") ($submit.data.correctCount -eq $QuestionCount) ($submit.data | ConvertTo-Json -Compress)
Check '正确率 100%' ([decimal]$submit.data.accuracy -eq 100) $submit.data.accuracy
Check '三星（stars=3）' ($submit.data.stars -eq 3) $submit.data.stars
Check ("获得积分 " + $ExpectedPoints) ($submit.data.pointsGained -eq $ExpectedPoints) ("actual=" + $submit.data.pointsGained)

Write-Host ("CHILD_ID=" + $childId) -ForegroundColor Yellow
Write-Host ("===== 结果：PASS=" + $script:pass + "  FAIL=" + $script:fail + " =====") -ForegroundColor Cyan
if ($script:fail -gt 0) { exit 1 }
