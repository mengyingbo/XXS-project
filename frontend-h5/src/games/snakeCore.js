/* eslint-disable */
/**
 * 贪吃蛇游戏核心（v2.3 阶段 11）
 * 纯 ES5 实现：仅用 var/function，无箭头函数、class、Promise、解构等 ES6+ 语法。
 * 以 IIFE 封装避免全局变量污染，仅通过模块导出一个工厂函数。
 * 画布坐标系：32 列 × 18 行（16:9），每格大小由画布宽度自动计算。
 */
var snakeCore = (function () {
    'use strict';

    var COLS = 32;              // 网格列数（决定 16:9 比例）
    var ROWS = 18;              // 网格行数
    var BASE_SPEED = 1000;      // 初始移动间隔（毫秒）= 每秒 1 格
    var SPEED_UP_EVERY = 5;     // 每吃 5 个食物提速一次
    var SPEED_FACTOR = 0.8;     // 提速 20% = 间隔乘 0.8
    var SCORE_PER_FOOD = 10;    // 每个食物得分
    var KEY_DEBOUNCE = 80;      // 键盘防抖间隔（毫秒）

    /** 方向 -> 坐标增量映射表 */
    var DIR_DELTA = {
        up: { x: 0, y: -1 },
        down: { x: 0, y: 1 },
        left: { x: -1, y: 0 },
        right: { x: 1, y: 0 }
    };

    /** 判断 newDir 是否与 dir 相反（防反向规则） */
    function isReverse(dir, newDir) {
        var a = DIR_DELTA[dir];
        var b = DIR_DELTA[newDir];
        return a.x + b.x === 0 && a.y + b.y === 0;
    }

    /**
     * 创建游戏实例
     * @param {Object} options
     * @param {HTMLCanvasElement} options.canvas 主画布
     * @param {Object}  options.hooks 状态回调：onScore/onOver/onSpeedUp/onStateChange
     * @returns 游戏控制接口 { start, togglePause, setDirection, destroy, getState }
     */
    function createGame(options) {
        var canvas = options.canvas;
        var hooks = options.hooks || {};
        var ctx = canvas.getContext('2d');

        /* ---------- 游戏状态 ---------- */
        var state = 'waiting';  // waiting | running | paused | over
        var snake = [];         // 蛇身数组，[{x,y}]，下标 0 为蛇头
        var dir = 'right';      // 当前实际移动方向
        var nextDir = 'right';  // 已排队方向（下一次移动时生效）
        var food = { x: 0, y: 0 };
        var score = 0;
        var eaten = 0;          // 已吃食物数（用于提速计数）
        var speedMs = BASE_SPEED;
        var timer = 0;          // setInterval 句柄
        var flashUntil = 0;     // 提速闪烁的结束时间戳
        var lastKeyTime = 0;    // 键盘防抖时间戳
        var lastQueued = 'right'; // 最近一次排队成功的方向（防抖去重）

        /* ---------- 画布尺寸（自适应 + 高清屏） ---------- */
        var cssW = 0;
        var cssH = 0;
        var dpr = 1;
        var cell = 0;

        function resize() {
            try {
                cssW = Math.min(window.innerWidth - 24, 560);
                cssH = Math.round(cssW * 9 / 16);
                dpr = Math.min(window.devicePixelRatio || 1, 2);
                canvas.style.width = cssW + 'px';
                canvas.style.height = cssH + 'px';
                canvas.width = Math.round(cssW * dpr);
                canvas.height = Math.round(cssH * dpr);
                cell = cssW / COLS;
                if (state !== 'running') draw();
            } catch (e) {
                /* 尺寸计算失败时保持上次画布，不中断游戏 */
            }
        }

        /* ---------- 初始化 / 重置 ---------- */

        /** 重置：3 节蛇、位于画布中心偏左、向右移动、速度回到初始值 */
        function reset() {
            var cx = Math.floor(COLS / 2);   // 中心偏左：蛇头在中心列，身体向左排开
            var cy = Math.floor(ROWS / 2);
            snake = [
                { x: cx, y: cy },
                { x: cx - 1, y: cy },
                { x: cx - 2, y: cy }
            ];
            dir = 'right';
            nextDir = 'right';
            lastQueued = 'right';
            score = 0;
            eaten = 0;
            speedMs = BASE_SPEED;
            flashUntil = 0;
            placeFood();
            if (hooks.onScore) hooks.onScore(score);
        }

        /**
         * 随机放置食物，避开蛇身占据的所有格子
         * @returns {boolean} 是否放置成功（蛇占满全盘时返回 false）
         */
        function placeFood() {
            var free = [];
            var occupied = {};
            var i;
            for (i = 0; i < snake.length; i++) {
                occupied[snake[i].x + ',' + snake[i].y] = true;
            }
            for (var y = 0; y < ROWS; y++) {
                for (var x = 0; x < COLS; x++) {
                    if (!occupied[x + ',' + y]) free.push({ x: x, y: y });
                }
            }
            if (free.length === 0) return false;
            food = free[Math.floor(Math.random() * free.length)];
            return true;
        }

        /* ---------- 移动与碰撞 ---------- */

        /** 移动一格：应用转向 -> 计算新蛇头 -> 碰撞检测 -> 吃食/缩尾 */
        function step() {
            // 转向在下一次移动时生效（此处才把排队方向变成实际方向）
            dir = nextDir;

            var d = DIR_DELTA[dir];
            var head = { x: snake[0].x + d.x, y: snake[0].y + d.y };

            // 1. 边界碰撞：碰到画布边缘即结束
            if (head.x < 0 || head.y < 0 || head.x >= COLS || head.y >= ROWS) {
                return gameOver();
            }

            // 2. 自身碰撞：新蛇头撞到身体（尾巴即将移走，故尾格不算）
            for (var i = 0; i < snake.length - 1; i++) {
                if (snake[i].x === head.x && snake[i].y === head.y) {
                    return gameOver();
                }
            }

            snake.unshift(head);

            // 3. 食物碰撞：得分 + 增长（不移除尾节即增长一节）
            if (head.x === food.x && head.y === food.y) {
                score += SCORE_PER_FOOD;
                eaten += 1;
                if (hooks.onScore) hooks.onScore(score);
                // 每吃 5 个食物提速 20%，并触发视觉闪烁反馈
                if (eaten % SPEED_UP_EVERY === 0) {
                    speedMs = Math.max(120, Math.floor(speedMs * SPEED_FACTOR));
                    flashUntil = Date.now() + 400;
                    restartTimer();
                    if (hooks.onSpeedUp) hooks.onSpeedUp();
                }
                if (!placeFood()) return gameOver(); // 蛇占满全盘，无地放食物
            } else {
                snake.pop(); // 未吃到食物：移除尾节
            }

            draw();
        }

        /* ---------- 定时器 ---------- */

        function restartTimer() {
            stopTimer();
            // 用 setInterval 驱动 tick；提速时重建定时器缩短间隔
            timer = setInterval(function () { step(); }, speedMs);
        }

        function stopTimer() {
            if (timer) {
                clearInterval(timer);
                timer = 0;
            }
        }

        /* ---------- 状态切换 ---------- */

        function start() {
            reset();
            state = 'running';
            if (hooks.onStateChange) hooks.onStateChange(state);
            draw();
            restartTimer();
        }

        function pause() {
            if (state !== 'running') return;
            state = 'paused';
            stopTimer();
            if (hooks.onStateChange) hooks.onStateChange(state);
            draw();
        }

        function resume() {
            if (state !== 'paused') return;
            state = 'running';
            if (hooks.onStateChange) hooks.onStateChange(state);
            restartTimer();
            draw();
        }

        function togglePause() {
            if (state === 'running') pause();
            else if (state === 'paused') resume();
        }

        function gameOver() {
            state = 'over';
            stopTimer();
            if (hooks.onStateChange) hooks.onStateChange(state);
            if (hooks.onOver) hooks.onOver(score);
            draw();
        }

        /* ---------- 方向输入（防反向 + 防抖） ---------- */

        /**
         * 设置方向：反向输入直接无效；同 tick 内重复/回摆输入去重；
         * 实际转向在下一次 step() 时才应用，避免瞬间转向自撞。
         * @param {string} newDir 'up'|'down'|'left'|'right'
         */
        function setDirection(newDir) {
            if (!DIR_DELTA.hasOwnProperty(newDir)) return;
            if (state !== 'running') return;
            // 防抖：与最近一次排队方向相同，或是它的反向，都忽略
            if (newDir === lastQueued || isReverse(lastQueued, newDir)) return;
            // 防反向：不能直接掉头
            if (isReverse(dir, newDir)) return;
            nextDir = newDir;
            lastQueued = newDir;
        }

        /* ---------- 绘制 ---------- */

        /**
         * 圆角矩形路径（iOS 11 无 ctx.roundRect，用 arcTo 手绘）
         * @param {number} r 圆角半径
         */
        function roundRectPath(c, x, y, w, h, r) {
            c.beginPath();
            c.moveTo(x + r, y);
            c.arcTo(x + w, y, x + w, y + h, r);
            c.arcTo(x + w, y + h, x, y + h, r);
            c.arcTo(x, y + h, x, y, r);
            c.arcTo(x, y, x + w, y, r);
            c.closePath();
        }

        /** 绘制一帧：背景 -> 网格 -> 食物 -> 蛇 -> 提速闪烁 */
        function draw() {
            if (!ctx) return;
            // 以 CSS 像素为逻辑坐标，按 dpr 放大保证高清屏清晰
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

            // 背景 #333333
            ctx.fillStyle = '#333333';
            ctx.fillRect(0, 0, cssW, cssH);

            // 网格线 #666666、透明度 0.3、间隔 10px（隐约可见）
            ctx.strokeStyle = 'rgba(102, 102, 102, 0.3)';
            ctx.lineWidth = 1;
            ctx.beginPath();
            for (var gx = 10; gx < cssW; gx += 10) {
                ctx.moveTo(gx + 0.5, 0);
                ctx.lineTo(gx + 0.5, cssH);
            }
            for (var gy = 10; gy < cssH; gy += 10) {
                ctx.moveTo(0, gy + 0.5);
                ctx.lineTo(cssW, gy + 0.5);
            }
            ctx.stroke();

            // 食物：纯红圆形，半径为格子的 40%
            ctx.fillStyle = '#FF0000';
            ctx.beginPath();
            ctx.arc(
                (food.x + 0.5) * cell,
                (food.y + 0.5) * cell,
                cell * 0.4,
                0,
                Math.PI * 2
            );
            ctx.fill();

            // 蛇：头 #00FF00、身 #00DD00，圆角矩形（圆角半径 10% 格宽）
            var radius = cell * 0.1;
            var inset = cell * 0.06; // 四周略缩进，格与格之间留缝更好看
            for (var i = snake.length - 1; i >= 0; i--) {
                ctx.fillStyle = i === 0 ? '#00FF00' : '#00DD00';
                roundRectPath(
                    ctx,
                    snake[i].x * cell + inset,
                    snake[i].y * cell + inset,
                    cell - inset * 2,
                    cell - inset * 2,
                    radius
                );
                ctx.fill();
            }

            // 提速反馈：画布短暂泛白闪烁
            if (Date.now() < flashUntil) {
                ctx.fillStyle = 'rgba(255, 255, 255, 0.25)';
                ctx.fillRect(0, 0, cssW, cssH);
            }
        }

        /* ---------- 键盘控制（方向键 + WASD，带防抖） ---------- */

        /** 键码 -> 方向 映射表（W A S D；方向键在 onKeyDown 的 switch 中单独处理） */
    var KEY_MAP = {
        87: 'up', 83: 'down', 65: 'left', 68: 'right'
    };
        function onKeyDown(e) {
            var d = null;
            switch (e.keyCode) {
                case 37: d = 'left'; break;  // ←
                case 38: d = 'up'; break;    // ↑
                case 39: d = 'right'; break; // →
                case 40: d = 'down'; break;  // ↓
                default: d = KEY_MAP[e.keyCode] || null;
            }
            if (!d) {
                // 空格：暂停/继续
                if (e.keyCode === 32) {
                    e.preventDefault();
                    togglePause();
                }
                return;
            }
            e.preventDefault(); // 阻止方向键滚动页面
            // 防抖：KEY_DEBOUNCE 毫秒内的重复按键忽略
            var now = Date.now();
            if (now - lastKeyTime < KEY_DEBOUNCE) return;
            lastKeyTime = now;
            if (state === 'waiting') {
                start();          // 未开始时按方向键直接开局
                setDirection(d);
            } else {
                setDirection(d);
            }
        }

        /* ---------- 事件绑定与生命周期 ---------- */

        function onResize() { resize(); }

        window.addEventListener('keydown', onKeyDown, false);
        window.addEventListener('resize', onResize, false);

        resize();
        reset();
        draw();

        return {
            start: start,
            togglePause: togglePause,
            setDirection: setDirection,
            getState: function () { return state; },
            getScore: function () { return score; },
            destroy: function () {
                stopTimer();
                window.removeEventListener('keydown', onKeyDown, false);
                window.removeEventListener('resize', onResize, false);
            }
        };
    }

    return {
        createGame: createGame
    };
})();

/* 浏览器直挂 window（供独立页面调试使用），默认导出工厂函数（供 Vue 项目导入） */
if (typeof window !== 'undefined') {
    window.SnakeCore = snakeCore;
}

export default snakeCore.createGame;
