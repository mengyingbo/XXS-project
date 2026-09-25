# =============================================================
# 阶段 2 接口自测脚本（孩子端 + 管理端全量）
# 用法：先启动后端（java -jar target/xxs-game-1.0.0.jar），再执行本脚本
#   powershell -ExecutionPolicy Bypass -File scripts/api-smoke-test.ps1
# 说明：脚本会创建一批以 _smoke 前缀的测试数据，运行结束后由
#       scripts/clean-smoke-data.sql 清理（见阶段 2 验收说明）
# 全部断言使用 ASCII 文案，另外单独做一次中文 UTF-8 往返校验
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

Write-Host "===== 0. 服务与鉴权 =====" -ForegroundColor Cyan
$health = Call GET '/health'
Check '健康检查 /api/health' ($health.code -eq 0 -and $health.data.db.connected) $health.message

$noToken = Call GET '/child/profile'
Check '无 Token 访问孩子端被拒(401)' ($noToken.code -eq 401) ($noToken | ConvertTo-Json -Compress)

Write-Host "===== 1. 管理端登录 / 改密 / Token 隔离 =====" -ForegroundColor Cyan
$login = Call POST '/admin/login' @{ username = 'admin'; password = 'admin123' }
Check '管理员默认账号登录' ($login.code -eq 0) $login.message
$admin = $login.data.token
Check '首次登录 mustChangePwd=true' ($login.data.admin.mustChangePwd -eq $true) ($login.data.admin | ConvertTo-Json -Compress)

$badLogin = Call POST '/admin/login' @{ username = 'admin'; password = 'wrong-pwd' }
Check '管理员密码错误被拒' ($badLogin.code -eq 401) $badLogin.message

$childTokenOnAdmin = Call GET '/admin/dashboard' $null 'eyJhbGciOiJIUzI1NiJ9.fake.token'
Check '伪造 Token 访问后台被拒(401)' ($childTokenOnAdmin.code -eq 401) $childTokenOnAdmin.message

$chg = Call PUT '/admin/password' @{ oldPassword = 'admin123'; newPassword = 'SmokePwd2026' } $admin
Check '修改管理员密码' ($chg.code -eq 0) $chg.message
$login2 = Call POST '/admin/login' @{ username = 'admin'; password = 'SmokePwd2026' }
Check '新密码可登录且 mustChangePwd=false' ($login2.code -eq 0 -and $login2.data.admin.mustChangePwd -eq $false) ($login2.data.admin | ConvertTo-Json -Compress)
$admin = $login2.data.token

$wrongOld = Call PUT '/admin/password' @{ oldPassword = 'admin123'; newPassword = 'Whatever2026' } $admin
Check '原密码错误时拒绝改密' ($wrongOld.code -eq 400) $wrongOld.message

Write-Host "===== 2. 管理端：单元 / 课文 / 关卡 =====" -ForegroundColor Cyan
$cn = ([char]0x6D4B + [char]0x8BD5 + [char]0x5355 + [char]0x5143)   # 测试单元
$unit = Call POST '/admin/unit' @{ unitNo = 99; title = $cn; description = 'smoke'; sortOrder = 9900 } $admin
Check '新增单元' ($unit.code -eq 0) $unit.message
$unitId = $unit.data.id

$unitList = Call GET '/admin/unit' $null $admin
$found = $unitList.data | Where-Object { $_.id -eq $unitId }
Check '中文标题 UTF-8 往返一致' ($found.title -eq $cn) ("expect=" + $cn + " actual=" + $found.title)

$dupUnit = Call POST '/admin/unit' @{ unitNo = 99; title = 'dup'; description = ''; sortOrder = 9901 } $admin
Check '单元序号重复被拒' ($dupUnit.code -eq 500 -or $dupUnit.code -eq 400) $dupUnit.message

$lesson = Call POST '/admin/lesson' @{ unitId = $unitId; lessonNo = 1; title = 'SmokeLesson'; lessonType = 'TEXT'; isSkim = $true; sortOrder = 1 } $admin
Check '新增课文' ($lesson.code -eq 0) $lesson.message
$lessonId = $lesson.data.id

$level1 = Call POST '/admin/level' @{ lessonId = $lessonId; levelNo = 1; name = 'SmokeL1'; questionCount = 4; sortOrder = 1 } $admin
Check '新增关卡 1' ($level1.code -eq 0) $level1.message
$level1Id = $level1.data.id

$level2 = Call POST '/admin/level' @{ lessonId = $lessonId; levelNo = 2; name = ''; questionCount = 1; sortOrder = 2 } $admin
Check '新增关卡 2' ($level2.code -eq 0) $level2.message
$level2Id = $level2.data.id

Write-Host "===== 3. 管理端：题目 CRUD 与校验 =====" -ForegroundColor Cyan
$q1 = Call POST '/admin/question' @{ lessonId = $lessonId; levelId = $level1Id; type = 'SINGLE'; stem = 'Smoke single'; options = '["A","B","C"]'; answer = '1'; analysis = 'because B'; knowledgePoint = 'KP1'; difficulty = 1; sortOrder = 1 } $admin
Check '新增单选题' ($q1.code -eq 0) $q1.message
$q1Id = $q1.data.id

$badQ = Call POST '/admin/question' @{ lessonId = $lessonId; levelId = $level1Id; type = 'SINGLE'; stem = 'bad'; options = '["A"]'; answer = '5'; analysis = ''; knowledgePoint = ''; difficulty = 1 } $admin
Check '单选题答案越界被拒' ($badQ.code -eq 400) $badQ.message

$badQ2 = Call POST '/admin/question' @{ lessonId = $lessonId; levelId = $level1Id; type = 'ORDER'; stem = 'bad order'; options = '["a","b","c"]'; answer = '[0,0,2]'; analysis = ''; knowledgePoint = ''; difficulty = 1 } $admin
Check '排序题答案非排列被拒' ($badQ2.code -eq 400) $badQ2.message

$q2 = Call POST '/admin/question' @{ lessonId = $lessonId; levelId = $level1Id; type = 'JUDGE'; stem = 'Smoke judge'; answer = 'true'; analysis = ''; knowledgePoint = 'KP2'; difficulty = 1; sortOrder = 2 } $admin
$q3 = Call POST '/admin/question' @{ lessonId = $lessonId; levelId = $level1Id; type = 'BLANK'; stem = 'Smoke blank'; options = $null; answer = '["chaoshui","chaoshu"]'; analysis = 'pinyin'; knowledgePoint = 'KP2'; difficulty = 2; sortOrder = 3 } $admin
$q4 = Call POST '/admin/question' @{ lessonId = $lessonId; levelId = $level1Id; type = 'ORDER'; stem = 'Smoke order'; options = '["one","two","three"]'; answer = '[2,0,1]'; analysis = 'order hint'; knowledgePoint = 'KP3'; difficulty = 2; sortOrder = 4 } $admin
Check '新增判断/填空/排序题' ($q2.code -eq 0 -and $q3.code -eq 0 -and $q4.code -eq 0) ($q2.message + '|' + $q3.message + '|' + $q4.message)
$q4Id = $q4.data.id

$q5 = Call POST '/admin/question' @{ lessonId = $lessonId; levelId = $level2Id; type = 'JUDGE'; stem = 'Smoke level2 question'; answer = 'true'; analysis = ''; knowledgePoint = 'KP1'; difficulty = 1; sortOrder = 1 } $admin
Check '新增关卡 2 的题目' ($q5.code -eq 0) $q5.message

$qList = Call GET "/admin/question?page=1&size=10&lessonId=$lessonId&type=JUDGE" $null $admin
Check '题目按课文+题型筛选' ($qList.code -eq 0 -and $qList.data.total -eq 2) ($qList.data | ConvertTo-Json -Compress)

$qSearch = Call GET "/admin/question?page=1&size=10&keyword=Smoke%20order" $null $admin
Check '题目按关键词搜索' ($qSearch.code -eq 0 -and $qSearch.data.total -eq 1) ($qSearch.data | ConvertTo-Json -Compress)

Write-Host "===== 4. 管理端：批量导入（预览 + 入库） =====" -ForegroundColor Cyan
$impBody = @{
    dryRun          = $true
    defaultLessonId = $lessonId
    defaultLevelId  = $level2Id
    rows            = @(
        @{ type = 'JUDGE'; stem = 'Import ok row'; answer = 'false'; analysis = ''; knowledgePoint = 'KP9'; difficulty = 1 },
        @{ type = 'SINGLE'; stem = 'Import bad row'; options = '["only-one"]'; answer = '9'; analysis = ''; knowledgePoint = ''; difficulty = 1 }
    )
}
$imp = Call POST '/admin/question/import' $impBody $admin
Check '导入预览返回逐行校验结果' ($imp.code -eq 0 -and $imp.data.validCount -eq 1 -and $imp.data.invalidCount -eq 1 -and $imp.data.inserted -eq 0) ($imp.data | ConvertTo-Json -Depth 5 -Compress)

$impBody.dryRun = $false
$imp2 = Call POST '/admin/question/import' $impBody $admin
Check '导入入库仅写入合法行' ($imp2.code -eq 0 -and $imp2.data.inserted -eq 1) ($imp2.data | ConvertTo-Json -Depth 4 -Compress)

$tpl = Call GET '/admin/question/import/template' $null $admin
Check 'CSV 模板可下载' ($tpl.code -eq 0 -and $tpl.data -like 'lessonId,levelId,type*') $tpl.message

Write-Host "===== 5. 孩子端：注册 / PIN 登录 / 锁定 =====" -ForegroundColor Cyan
$childList = Call GET '/child/list'
Check '档案列表（免登录）' ($childList.code -eq 0) $childList.message

$reg = Call POST '/child/register' @{ nickname = 'SmokeBaby'; avatar = 'cat'; pin = '2468' }
Check '新建孩子档案' ($reg.code -eq 0) $reg.message
$childId = $reg.data.id

$badPin1 = Call POST '/child/login' @{ childId = $childId; pin = '0000' }
Check 'PIN 错误提示剩余次数' ($badPin1.code -eq 403) $badPin1.message
$badPin2 = Call POST '/child/login' @{ childId = $childId; pin = '0000' }
Check 'PIN 连续错误计数' ($badPin2.code -eq 403) $badPin2.message

$cLogin = Call POST '/child/login' @{ childId = $childId; pin = '2468' }
Check 'PIN 正确登录' ($cLogin.code -eq 0) $cLogin.message
$child = $cLogin.data.token

for ($i = 0; $i -lt 5; $i++) { $locked = Call POST '/child/login' @{ childId = $childId; pin = '0000' } }
Check 'PIN 错 5 次触发锁定(403)' ($locked.code -eq 403) $locked.message
$lockedOk = Call POST '/child/login' @{ childId = $childId; pin = '2468' }
Check '锁定期间正确 PIN 也被拒' ($lockedOk.code -eq 403) $lockedOk.message

$adminTokenOnChild = Call GET '/child/profile' $null $admin
Check 'Admin Token 访问孩子端被拒(401)' ($adminTokenOnChild.code -eq 401) $adminTokenOnChild.message

$resetPin = Call PUT "/admin/child/$childId/pin" @{ pin = '2468' } $admin
Check '后台重置 PIN 同时解除锁定' ($resetPin.code -eq 0) $resetPin.message
$cLogin2 = Call POST '/child/login' @{ childId = $childId; pin = '2468' }
Check '解锁后可重新登录' ($cLogin2.code -eq 0) $cLogin2.message
$child = $cLogin2.data.token

Write-Host "===== 6. 孩子端：地图与解锁 =====" -ForegroundColor Cyan
$profile = Call GET '/child/profile' $null $child
Check '个人信息含今日额度' ($profile.code -eq 0 -and $profile.data.today.remainingQuestions -eq 20 -and $profile.data.today.remainingMinutes -eq 20) ($profile.data | ConvertTo-Json -Depth 4 -Compress)

$map = Call GET '/child/map' $null $child
$u = $map.data.units | Where-Object { $_.id -eq $unitId }
$l1 = $u.lessons[0].levels | Where-Object { $_.id -eq $level1Id }
$l2 = $u.lessons[0].levels | Where-Object { $_.id -eq $level2Id }
Check '地图首关默认解锁' ($l1.status -eq 'UNLOCKED') $l1.status
Check '地图第二关默认锁定' ($l2.status -eq 'LOCKED') $l2.status
Check '地图返回当前可闯关卡' ($map.data.currentLevelId -eq $level1Id) $map.data.currentLevelId

$startLocked = Call POST '/child/session/start' @{ levelId = $level2Id } $child
Check '未解锁关卡不能开始' ($startLocked.code -eq 409) $startLocked.message

Write-Host "===== 7. 孩子端：答题与结算 =====" -ForegroundColor Cyan
$start = Call POST '/child/session/start' @{ levelId = $level1Id } $child
Check '开始关卡返回 4 道题' ($start.code -eq 0 -and $start.data.questionCount -eq 4) ($start.data | ConvertTo-Json -Depth 3 -Compress)
$hasAnswer = $null -ne $start.data.questions[0].PSObject.Properties['answer']
Check '开始关卡不泄露答案' (-not $hasAnswer) 'answer field leaked'

$sub1 = Call POST '/child/session/submit' @{
    levelId    = $level1Id
    durationMs = 60000
    answers    = @(
        @{ questionId = $q1Id; userAnswer = '1'; durationMs = 5000 },
        @{ questionId = $q2.data.id; userAnswer = 'true'; durationMs = 5000 },
        @{ questionId = $q3.data.id; userAnswer = 'chaoshui'; durationMs = 5000 },
        @{ questionId = $q4Id; userAnswer = '[0,1,2]'; durationMs = 5000 }
    )
} $child
Check '结算：对 3 错 1 = 75%' ($sub1.code -eq 0 -and $sub1.data.correctCount -eq 3 -and $sub1.data.accuracy -eq 75) ($sub1.data | ConvertTo-Json -Depth 3 -Compress)
Check '结算：75% 判定二星并通关' ($sub1.data.stars -eq 2 -and $sub1.data.passed) ('stars=' + $sub1.data.stars + ' passed=' + $sub1.data.passed)
Check '结算：首次通关积分 = 30+5+20 = 55' ($sub1.data.pointsGained -eq 55 -and $sub1.data.points -eq 55) ('gained=' + $sub1.data.pointsGained + ' total=' + $sub1.data.points)
Check '结算：连对 3 题加成 5 分' ($sub1.data.pointsBreakdown.combo -eq 5) ($sub1.data.pointsBreakdown | ConvertTo-Json -Compress)
Check '结算：提示解锁下一关' ($sub1.data.nextLevelId -eq $level2Id) $sub1.data.nextLevelId
Check '结算：返回逐题解析' ($sub1.data.details.Count -eq 4 -and $sub1.data.details[0].analysis -eq 'because B') $sub1.data.details[0].analysis

$wrong = Call GET '/child/wrong-questions' $null $child
Check '错题本记录答错的排序题' ($wrong.code -eq 0 -and $wrong.data.Count -eq 1 -and $wrong.data[0].questionId -eq $q4Id) ($wrong.data | ConvertTo-Json -Depth 2 -Compress)

$map2 = Call GET '/child/map' $null $child
$u2 = $map2.data.units | Where-Object { $_.id -eq $unitId }
$l2b = $u2.lessons[0].levels | Where-Object { $_.id -eq $level2Id }
Check '通关后下一关解锁' ($l2b.status -eq 'UNLOCKED') $l2b.status
Check '地图统计星数与通关数' ($map2.data.totalStars -eq 2 -and $map2.data.passedLevels -eq 1) ($map2.data.totalStars.ToString() + '/' + $map2.data.passedLevels)

$sub2 = Call POST '/child/session/submit' @{
    levelId    = $level1Id
    durationMs = 60000
    answers    = @(
        @{ questionId = $q1Id; userAnswer = '1'; durationMs = 5000 },
        @{ questionId = $q2.data.id; userAnswer = 'true'; durationMs = 5000 },
        @{ questionId = $q3.data.id; userAnswer = '"chaoshui"'; durationMs = 5000 },
        @{ questionId = $q4Id; userAnswer = '[2,0,1]'; durationMs = 5000 }
    )
} $child
Check '重复挑战全对 = 100% 三星' ($sub2.code -eq 0 -and $sub2.data.stars -eq 3 -and $sub2.data.accuracy -eq 100) ($sub2.data | ConvertTo-Json -Depth 3 -Compress)
Check '重复通关不再发通关奖励' ($sub2.data.pointsBreakdown.passBonus -eq 0) ($sub2.data.pointsBreakdown | ConvertTo-Json -Compress)
Check '首次三星发放 30 分奖励' ($sub2.data.pointsBreakdown.threeStarBonus -eq 30) ($sub2.data.pointsBreakdown | ConvertTo-Json -Compress)
Check '积分累计 = 55+75 = 130' ($sub2.data.points -eq 130) $sub2.data.points

$wrong2 = Call GET '/child/wrong-questions' $null $child
Check '订正后错题本清空' ($wrong2.code -eq 0 -and $wrong2.data.Count -eq 0) ($wrong2.data | ConvertTo-Json -Depth 2 -Compress)

$start2 = Call POST '/child/session/start' @{ levelId = $level2Id } $child
Check '解锁后可开始第 2 关' ($start2.code -eq 0 -and $start2.data.questionCount -eq 1) $start2.message
$sub3 = Call POST '/child/session/submit' @{
    levelId = $level2Id
    answers = @(@{ questionId = $q5.data.id; userAnswer = 'true'; durationMs = 3000 })
} $child
Check '第 2 关满分通关（10+20+30=60）' ($sub3.data.pointsGained -eq 60 -and $sub3.data.points -eq 190) ('gained=' + $sub3.data.pointsGained + ' total=' + $sub3.data.points)
Check '全部通关后无下一关' ($sub3.data.nextLevelId -eq $null) $sub3.data.nextLevelId

$profile2 = Call GET '/child/profile' $null $child
Check '今日已答题数 9 / 剩余 11' ($profile2.data.today.answeredToday -eq 9 -and $profile2.data.today.remainingQuestions -eq 11) ($profile2.data.today | ConvertTo-Json -Compress)

Write-Host "===== 8. 孩子端：每日额度拦截 =====" -ForegroundColor Cyan
$cfg = Call GET '/admin/config' $null $admin
Check '读取规则配置' ($cfg.code -eq 0 -and $cfg.data.Count -eq 13) $cfg.data.Count

Call PUT '/admin/config' @{ daily_question_limit = '5' } $admin | Out-Null
$blockedQ = Call POST '/child/session/start' @{ levelId = $level1Id } $child
Check '超出每日题量上限被拦截' ($blockedQ.code -eq 409) $blockedQ.message
Call PUT '/admin/config' @{ daily_question_limit = '20' } $admin | Out-Null

Call PUT '/admin/config' @{ daily_minute_limit = '0' } $admin | Out-Null
$blockedM = Call POST '/child/session/start' @{ levelId = $level1Id } $child
Check '超出每日时长上限被拦截' ($blockedM.code -eq 409) $blockedM.message
Call PUT '/admin/config' @{ daily_minute_limit = '20' } $admin | Out-Null

Write-Host "===== 9. 孩子端：积分商城与兑换 =====" -ForegroundColor Cyan
$prize = Call POST '/admin/prize' @{ name = 'SmokePrize'; image = ''; pointsCost = 50; stock = 1; enabled = $true; description = 'smoke'; sortOrder = 1 } $admin
Check '后台新增奖品' ($prize.code -eq 0) $prize.message
$prizeId = $prize.data.id

$prizeList = Call GET '/child/prize/list' $null $child
$p = $prizeList.data.prizes | Where-Object { $_.id -eq $prizeId }
Check '奖品列表返回可兑换标记' ($prizeList.code -eq 0 -and $p.canRedeem -eq $true) ($prizeList.data | ConvertTo-Json -Depth 3 -Compress)

$redeem = Call POST '/child/prize/redeem' @{ prizeId = $prizeId } $child
Check '兑换扣积分并生成待审核订单' ($redeem.code -eq 0 -and $redeem.data.status -eq 'PENDING' -and $redeem.data.points -eq 140) ($redeem.data | ConvertTo-Json -Compress)

$redeem2 = Call POST '/child/prize/redeem' @{ prizeId = $prizeId } $child
Check '库存不足时拒绝兑换' ($redeem2.code -eq 409) $redeem2.message

Write-Host "===== 10. 管理端：兑换审核 =====" -ForegroundColor Cyan
$redeems = Call GET '/admin/redeem?status=PENDING' $null $admin
Check '待审核兑换列表' ($redeems.code -eq 0 -and $redeems.data.total -eq 1 -and $redeems.data.records[0].childNickname -eq 'SmokeBaby') ($redeems.data | ConvertTo-Json -Depth 3 -Compress)

$approve = Call PUT "/admin/redeem/$($redeem.data.orderId)" @{ action = 'APPROVE'; remark = 'ok' } $admin
Check '审核通过' ($approve.code -eq 0 -and $approve.data.status -eq 'APPROVED') $approve.message
$deliver = Call PUT "/admin/redeem/$($redeem.data.orderId)" @{ action = 'DELIVER'; remark = 'handed' } $admin
Check '标记已发放' ($deliver.code -eq 0 -and $deliver.data.status -eq 'DELIVERED') $deliver.message
$rejectDelivered = Call PUT "/admin/redeem/$($redeem.data.orderId)" @{ action = 'REJECT'; remark = '' } $admin
Check '已发放订单不能拒绝' ($rejectDelivered.code -eq 409) $rejectDelivered.message

# 先补充库存，再提一笔新的兑换用于验证"拒绝退回积分"
Call PUT "/admin/prize/$prizeId" @{ name = 'SmokePrize'; image = ''; pointsCost = 50; stock = 3; enabled = $true; description = 'smoke'; sortOrder = 1 } $admin | Out-Null
$redeem3 = Call POST '/child/prize/redeem' @{ prizeId = $prizeId } $child
Check '补库存后可再次兑换' ($redeem3.code -eq 0 -and $redeem3.data.status -eq 'PENDING') $redeem3.message
$rej = Call PUT "/admin/redeem/$($redeem3.data.orderId)" @{ action = 'REJECT'; remark = 'no stock' } $admin
Check '拒绝兑换退回积分' ($rej.code -eq 0 -and $rej.data.points -eq 140) ($rej.data | ConvertTo-Json -Compress)

Write-Host "===== 11. 管理端：孩子管理与调分 =====" -ForegroundColor Cyan
$adminChildren = Call GET "/admin/child?page=1&size=10&keyword=Smoke" $null $admin
Check '后台孩子列表支持搜索' ($adminChildren.code -eq 0 -and $adminChildren.data.total -eq 1) ($adminChildren.data | ConvertTo-Json -Depth 2 -Compress)

$adj = Call PUT "/admin/child/$childId/points" @{ delta = -10; remark = 'smoke adjust down' } $admin
Check '调分（减 10）并写流水' ($adj.code -eq 0 -and $adj.data.points -eq 130) ($adj.data | ConvertTo-Json -Compress)
$adj2 = Call PUT "/admin/child/$childId/points" @{ delta = -9999; remark = 'over' } $admin
Check '调分不能把积分调成负数' ($adj2.code -eq 400) $adj2.message

$records = Call GET '/child/records' $null $child
Check '我的：统计正确率' ($records.code -eq 0 -and $records.data.statistics.answeredTotal -eq 9 -and $records.data.statistics.correctTotal -eq 8) ($records.data.statistics | ConvertTo-Json -Compress)
$hasRedeemLog = ($records.data.pointLogs | Where-Object { $_.bizType -eq 'REDEEM' }) -ne $null
$hasRefundLog = ($records.data.pointLogs | Where-Object { $_.bizType -eq 'REJECT_REFUND' }) -ne $null
$hasAdjustLog = ($records.data.pointLogs | Where-Object { $_.bizType -eq 'ADMIN_ADJUST' }) -ne $null
Check '积分流水含兑换/退回/家长调整' ($hasRedeemLog -and $hasRefundLog -and $hasAdjustLog) 'missing biz types'
Check '兑换记录含已发放订单' ($records.data.redeemOrders.Count -eq 2) $records.data.redeemOrders.Count

$resetProgress = Call DELETE "/admin/child/$childId/progress/$level1Id" $null $admin
Check '后台重置某关进度' ($resetProgress.code -eq 0 -and $resetProgress.data.reset -eq 1) ($resetProgress.data | ConvertTo-Json -Compress)
$map3 = Call GET '/child/map' $null $child
$u3 = $map3.data.units | Where-Object { $_.id -eq $unitId }
$l1c = $u3.lessons[0].levels | Where-Object { $_.id -eq $level1Id }
$l2c = $u3.lessons[0].levels | Where-Object { $_.id -eq $level2Id }
Check '重置后第 1 关回到已解锁且星级/次数清零' ($l1c.status -eq 'UNLOCKED' -and $l1c.stars -eq 0 -and $l1c.attemptCount -eq 0) ($l1c | ConvertTo-Json -Compress)
Check '重置某关不影响后续关卡已通关状态' ($l2c.status -eq 'PASSED') $l2c.status

Write-Host "===== 12. 管理端：仪表盘与统计 =====" -ForegroundColor Cyan
$dash = Call GET '/admin/dashboard' $null $admin
Check '仪表盘汇总' ($dash.code -eq 0 -and $dash.data.enabledChildCount -eq 1 -and $dash.data.todayAnswers -eq 9 -and $dash.data.totalIssuedPoints -eq 190) ($dash.data | ConvertTo-Json -Depth 3 -Compress)

$stats = Call GET "/admin/stats?childId=$childId" $null $admin
Check '按孩子统计' ($stats.code -eq 0 -and $stats.data.byChild.Count -ge 1) ($stats.data.byChild | ConvertTo-Json -Depth 2 -Compress)
Check '按课文统计' ($stats.data.byLesson.Count -eq 1 -and $stats.data.byLesson[0].total -eq 9) ($stats.data.byLesson | ConvertTo-Json -Depth 2 -Compress)
Check '按知识点统计' ($stats.data.byKnowledgePoint.Count -eq 3) ($stats.data.byKnowledgePoint | ConvertTo-Json -Depth 2 -Compress)

Write-Host "===== 13. 管理端：上传与删除保护 =====" -ForegroundColor Cyan
$png = [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4, 0x89,
    0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
    0x0D, 0x0A, 0x2D, 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82)
$tmp = Join-Path $env:TEMP 'smoke-1x1.png'
[System.IO.File]::WriteAllBytes($tmp, $png)
$up = curl.exe -s -X POST "http://127.0.0.1:8080/api/admin/upload" -H "Authorization: Bearer $admin" -F "file=@$tmp" | ConvertFrom-Json
Check '上传奖品图片（白名单）' ($up.code -eq 0 -and $up.data.url -like '/api/files/*') ($up | ConvertTo-Json -Compress)
$badFile = Join-Path $env:TEMP 'smoke-bad.txt'
Set-Content -Path $badFile -Value 'not an image'
$up2 = curl.exe -s -X POST "http://127.0.0.1:8080/api/admin/upload" -H "Authorization: Bearer $admin" -F "file=@$badFile" | ConvertFrom-Json
Check '非图片类型被拒绝' ($up2.code -eq 400) ($up2 | ConvertTo-Json -Compress)

$delLevel = Call DELETE "/admin/level/$level1Id" $null $admin
Check '有题目时禁止删除关卡' ($delLevel.code -eq 409) $delLevel.message
$delUnit = Call DELETE "/admin/unit/$unitId" $null $admin
Check '有课文时禁止删除单元' ($delUnit.code -eq 409) $delUnit.message
$delLesson = Call DELETE "/admin/lesson/$lessonId" $null $admin
Check '有内容时禁止删除课文' ($delLesson.code -eq 409) $delLesson.message

$delQ = Call DELETE "/admin/question/$q1Id" $null $admin
Check '有作答记录的题目禁止删除' ($delQ.code -eq 409) $delQ.message

Write-Host ""
Write-Host ("===== 结果：通过 " + $script:pass + " 项，失败 " + $script:fail + " 项 =====") -ForegroundColor Cyan
if ($script:fail -gt 0) { exit 1 } else { exit 0 }