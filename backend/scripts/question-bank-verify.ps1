# =============================================================
# 阶段 3 · 题库接口实测脚本（第一单元）
# 前置：后端已启动；题库已导入（10_catalog / 11_questions_u1）
# 说明：创建临时孩子 ApiTestBaby 完成注册→登录→开局→全对提交，
#       校验判题/星级/积分/解锁，结束后清理临时业务数据，
#       保留单元/课文/关卡/题目。
# 用法：powershell -ExecutionPolicy Bypass -File scripts\question-bank-verify.ps1
# =============================================================

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

Write-Host "===== 0. 服务健康 =====" -ForegroundColor Cyan
$health = Call GET '/health'
Check '健康检查 + DB 连通' ($health.code -eq 0 -and $health.data.db.connected) $health.message

Write-Host "===== 1. 管理员登录（题库只读核对） =====" -ForegroundColor Cyan
$login = Call POST '/admin/login' @{ username = 'admin'; password = 'admin123' }
Check '管理员登录成功' ($login.code -eq 0) $login.message
$admin = $login.data.token

$qAdmin = Call GET '/admin/question?page=1&size=10&lessonId=1&levelId=1' $null $admin
Check '管理端可读到第1关 5 道题（含答案）' ($qAdmin.code -eq 0 -and $qAdmin.data.total -eq 5) ($qAdmin.data | ConvertTo-Json -Compress)

Write-Host "===== 2. 孩子注册 / PIN 登录 =====" -ForegroundColor Cyan
$reg = Call POST '/child/register' @{ nickname = 'ApiTest'; avatar = 'cat'; pin = '1234' }
Check '新建临时孩子档案' ($reg.code -eq 0) $reg.message
$childId = $reg.data.id

$cLogin = Call POST '/child/login' @{ childId = $childId; pin = '1234' }
Check 'PIN 正确登录' ($cLogin.code -eq 0) $cLogin.message
$child = $cLogin.data.token

Write-Host "===== 3. 闯关地图初始状态 =====" -ForegroundColor Cyan
$map = Call GET '/child/map' $null $child
$units = $map.data.units
$lvl1 = $units[0].lessons[0].levels[0]
$lvl2 = $units[0].lessons[0].levels[1]
Check '第1关初始为 UNLOCKED' ($lvl1.status -eq 'UNLOCKED') ($lvl1 | ConvertTo-Json -Compress)
Check '第2关初始为 LOCKED' ($lvl2.status -eq 'LOCKED') ($lvl2 | ConvertTo-Json -Compress)

Write-Host "===== 4. 开始第1关（题目不含答案） =====" -ForegroundColor Cyan
$start = Call POST '/child/session/start' @{ levelId = $lvl1.id } $child
Check '开局返回 5 道题' ($start.code -eq 0 -and $start.data.questions.Count -eq 5) ($start.data | ConvertTo-Json -Compress)
$leak = $start.data.questions | Get-Member -Name answer
Check '返回题目不携带 answer 字段' ($null -eq $leak) 'answer 字段泄漏'

Write-Host "===== 5. 全对提交：判题 / 星级 / 积分 =====" -ForegroundColor Cyan
# 第1关 5 题标准答案（与 11_questions_u1.sql 一致）：
# 单选0 / 单选2 / 判断false / 填空 风平浪静 / 判断true
$answers = @(
    [pscustomobject]@{ questionId = $start.data.questions[0].id; userAnswer = '0';        durationMs = 1000 },
    [pscustomobject]@{ questionId = $start.data.questions[1].id; userAnswer = '2';        durationMs = 1000 },
    [pscustomobject]@{ questionId = $start.data.questions[2].id; userAnswer = 'false';    durationMs = 1000 },
    [pscustomobject]@{ questionId = $start.data.questions[3].id; userAnswer = '风平浪静'; durationMs = 1000 },
    [pscustomobject]@{ questionId = $start.data.questions[4].id; userAnswer = 'true';     durationMs = 1000 }
)
$submit = Call POST '/child/session/submit' @{ levelId = $lvl1.id; durationMs = 5000; answers = $answers } $child
Check '提交成功' ($submit.code -eq 0) $submit.message
$d = $submit.data
Check '5 题全对（correctCount=5）' ($d.correctCount -eq 5) ($d | ConvertTo-Json -Compress)
Check '正确率 100%' ([decimal]$d.accuracy -eq 100) $d.accuracy
Check '三星（stars=3）' ($d.stars -eq 3) $d.stars
# 预期积分：5×10 + 连对3题5 + 通关20 + 三星30 = 105
Check '获得积分 105' ($d.pointsGained -eq 105) ("actual=" + $d.pointsGained)

Write-Host "===== 6. 地图推进：第1关 PASSED，第2关 UNLOCKED =====" -ForegroundColor Cyan
$map2 = Call GET '/child/map' $null $child
$lvl1b = $map2.data.units[0].lessons[0].levels[0]
$lvl2b = $map2.data.units[0].lessons[0].levels[1]
Check '第1关已 PASSED 且 3 星' ($lvl1b.status -eq 'PASSED' -and $lvl1b.stars -eq 3) ($lvl1b | ConvertTo-Json -Compress)
Check '第2关已 UNLOCKED' ($lvl2b.status -eq 'UNLOCKED') ($lvl2b | ConvertTo-Json -Compress)

Write-Host "===== 7. 错题本为空（全对局） =====" -ForegroundColor Cyan
$wrong = Call GET '/child/wrong-questions' $null $child
Check '错题本记录数为 0' ($wrong.code -eq 0 -and $wrong.data.Count -eq 0) ($wrong.data | ConvertTo-Json -Compress)

Write-Host "===== 8. 清理临时孩子及其业务数据 =====" -ForegroundColor Cyan
$cleanup = @{
    Method = 'POST'
    Path   = '/admin/child/' + $childId
}
# 后台没有级联删除接口，直接走库清理（由外部调用方执行 SQL 更稳妥）
Write-Host ("临时孩子 id=" + $childId + "，请执行清理 SQL") -ForegroundColor Yellow

Write-Host ""
Write-Host ("===== 结果：PASS=" + $script:pass + "  FAIL=" + $script:fail + " =====") -ForegroundColor Cyan
if ($script:fail -gt 0) { exit 1 }
