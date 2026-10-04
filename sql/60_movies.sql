-- ============================================================
-- 电影片库数据迁移（需求文档 07 F-AD-12）
-- 将 movieData.ts 中 50 部电影迁移到 movie 表
-- 执行前请先执行建表语句（见文档 07 第 3.1 节）
-- ============================================================

-- 建表
CREATE TABLE IF NOT EXISTS movie (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  no          INT          NOT NULL DEFAULT 0     COMMENT '显示序号（排序用）',
  name        VARCHAR(100) NOT NULL                COMMENT '电影名称',
  type        VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '类型',
  duration    INT          NOT NULL DEFAULT 0      COMMENT '片长（分钟）',
  rating      DECIMAL(3,1) NOT NULL DEFAULT 0.0   COMMENT '豆瓣评分',
  theme       VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '推荐主题',
  note        VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '适龄看点',
  cover       VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '封面图URL',
  watched     TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '默认观看状态（0未看/1已看）',
  enabled     TINYINT(1)   NOT NULL DEFAULT 1     COMMENT '是否上架（0下架/1上架）',
  sort_order  INT          NOT NULL DEFAULT 0     COMMENT '排序权重（升序）',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_enabled_sort (enabled, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电影片库';

-- 清空已有数据（首次执行可跳过）
TRUNCATE TABLE movie;

-- 50 部电影数据
INSERT INTO movie (no, name, type, duration, rating, theme, note, cover, watched, enabled, sort_order) VALUES
(1,  '放牛班的春天',         '成长/音乐', 97,  9.3, '包容与赏识', '用合唱唤醒问题学生，比说教更动人', '/movie-covers/cover-1.png',  1, 1, 1),
(2,  '海蒂和爷爷',           '成长/亲情', 111, 9.3, '乐观与真诚', '阿尔卑斯山风光治愈，教会孩子真诚待人', '/movie-covers/cover-2.png',  1, 1, 2),
(3,  '寻梦环游记',           '动画/亲情', 105, 9.1, '亲情与梦想', '被遗忘才是真正的消失，画面唯美', '/movie-covers/cover-3.png',  1, 1, 3),
(4,  '疯狂动物城',           '动画/励志', 109, 9.2, '梦想与坚持', '努力才能实现不可能，适合聊偏见与成见', '/movie-covers/cover-4.png',  1, 1, 4),
(5,  '千与千寻',             '动画/奇幻', 125, 9.4, '勇气与成长', '宫崎骏经典，独立面对陌生世界', '/movie-covers/cover-5.png',  1, 1, 5),
(6,  '龙猫',                 '动画/奇幻', 86,  9.2, '童年与想象', '纯真童年与自然之美，画面温暖', '/movie-covers/cover-6.png',  0, 1, 6),
(7,  '天空之城',             '动画/冒险', 124, 9.2, '友谊与冒险', '拉普达的冒险，关于守护与贪婪', '/movie-covers/cover-7.png',  0, 1, 7),
(8,  '哈尔的移动城堡',       '动画/奇幻', 119, 9.1, '爱与接纳',   '真正的美来自内心，反战主题温和', '/movie-covers/cover-8.png',  1, 1, 8),
(9,  '奇迹男孩',             '成长/校园', 113, 8.6, '善良与包容', '选择善良，聊校园接纳特别合适', '/movie-covers/cover-9.png',  1, 1, 9),
(10, '头脑特工队',           '动画/成长', 95,  8.8, '情绪启蒙',   '悲伤也重要，帮孩子疏导情绪', '/movie-covers/cover-10.png', 0, 1, 10),
(11, '飞屋环游记',           '动画/冒险', 96,  9.1, '爱与冒险',   '开头十分钟就让人落泪，讲陪伴', '/movie-covers/cover-11.png', 1, 1, 11),
(12, '机器人总动员',         '动画/科幻', 98,  9.3, '环保与勇气', '几乎没有对白的浪漫科幻', '/movie-covers/cover-12.png', 1, 1, 12),
(13, '忠犬八公的故事',       '剧情/动物', 93,  9.4, '忠诚与陪伴', '催泪但温暖，理解什么是等待', '/movie-covers/cover-13.png', 0, 1, 13),
(14, '小鞋子',               '剧情/家庭', 89,  9.2, '亲情与担当', '贫寒兄妹的换鞋接力，珍惜当下', '/movie-covers/cover-14.png', 1, 1, 14),
(15, '地球上的星星',         '成长/教育', 165, 8.9, '发现天赋',   '成绩不能定义一个人，老师视角', '/movie-covers/cover-15.png', 1, 1, 15),
(16, '当幸福来敲门',         '剧情/励志', 117, 9.2, '坚持与父爱', '逆境中的父爱，适合聊努力的意义', '/movie-covers/cover-16.png', 0, 1, 16),
(17, '美丽人生',             '剧情/二战', 116, 9.6, '父爱与乐观', '父亲用游戏保护孩子，需家长陪同看', '/movie-covers/cover-17.png', 0, 1, 17),
(18, '摔跤吧！爸爸',         '体育/励志', 140, 9.0, '拼搏与梦想', '女性力量，聊聊坚持与偏见', '/movie-covers/cover-18.png', 1, 1, 18),
(19, '哪吒之魔童降世',       '动画/神话', 110, 8.4, '打破偏见',   '我命由我不由天，国漫之光', '/movie-covers/cover-19.png', 1, 1, 19),
(20, '雄狮少年',             '动画/成长', 104, 8.4, '热血与坚持', '舞狮少年的追梦路，国风励志', '/movie-covers/cover-20.png', 0, 1, 20),
(21, '长安三万里',           '动画/历史', 168, 8.3, '诗词与家国', '48首唐诗串起大唐，语文加分', '/movie-covers/cover-21.png', 1, 1, 21),
(22, '天书奇谭',             '动画/神话', 89,  9.2, '经典国漫',   '国产美术片经典，故事生动幽默', '/movie-covers/cover-22.png', 0, 1, 22),
(23, '大闹天宫',             '动画/神话', 114, 9.4, '经典国漫',   '上海美影厂经典，孙悟空不畏强权', '/movie-covers/cover-23.png', 1, 1, 23),
(24, '狮子王',               '动画/成长', 89,  9.1, '责任与成长', '哈姆雷特式的狮子故事，生生不息', '/movie-covers/cover-24.png', 1, 1, 24),
(25, '海底总动员',           '动画/冒险', 100, 8.5, '父爱与勇气', '寻找尼莫，聊恐惧与勇敢', '/movie-covers/cover-25.png', 1, 1, 25),
(26, '玩具总动员',           '动画/友情', 81,  8.7, '友情与告别', '陪伴与放手，系列都可看', '/movie-covers/cover-26.png', 1, 1, 26),
(27, '冰雪奇缘',             '动画/奇幻', 102, 8.4, '姐妹与自我', '做自己，经典歌曲朗朗上口', '/movie-covers/cover-27.png', 1, 1, 27),
(28, '超能陆战队',           '动画/科幻', 102, 8.8, '科技与温暖', '大白治愈，聊聊科技向善', '/movie-covers/cover-28.png', 1, 1, 28),
(29, '驯龙高手',             '动画/冒险', 98,  8.8, '理解与共情', '跨越偏见成为朋友', '/movie-covers/cover-29.png', 1, 1, 29),
(30, '神偷奶爸',             '动画/喜剧', 95,  8.6, '亲情与改变', '小黄人爆笑，坏蛋也能变好爸爸', '/movie-covers/cover-30.png', 1, 1, 30),
(31, '神隐少女',             '动画/奇幻', 125, 9.4, '成长与勇气', '即千与千寻，日本动画巅峰', '/movie-covers/cover-31.png', 0, 1, 31),
(32, '红猪',                 '动画/冒险', 94,  8.6, '梦想与自由', '宫崎骏的中年浪漫，适合大孩子', '/movie-covers/cover-32.png', 0, 1, 32),
(33, '风之谷',               '动画/科幻', 116, 8.9, '环保与和平', '末日世界里的希望，反战反杀戮', '/movie-covers/cover-33.png', 0, 1, 33),
(34, '幽灵公主',             '动画/奇幻', 133, 8.9, '人与自然',   '探讨人与自然的冲突，稍深刻', '/movie-covers/cover-34.png', 0, 1, 34),
(35, '借东西的小人阿莉埃蒂', '动画/奇幻', 94,  8.8, '微小与勇敢', '借物一族的秘密生活', '/movie-covers/cover-35.png', 0, 1, 35),
(36, '侧耳倾听',             '动画/青春', 111, 8.9, '梦想与努力', '为梦想努力的青春故事', '/movie-covers/cover-36.png', 0, 1, 36),
(37, '悬崖上的金鱼姬',       '动画/奇幻', 101, 8.6, '纯真与承诺', '波妞与宗介的童真之约', '/movie-covers/cover-37.png', 1, 1, 37),
(38, '夏日友晴天',           '动画/成长', 95,  8.4, '友谊与接纳', '海怪男孩的人类世界冒险', '/movie-covers/cover-38.png', 0, 1, 38),
(39, '1/2的魔法',            '动画/奇幻', 102, 7.9, '亲情与成长', '一对兄弟的魔法冒险', '/movie-covers/cover-39.png', 1, 1, 39),
(40, '心灵奇旅',             '动画/成长', 100, 8.7, '热爱与生活', '好好感受当下的每一刻', '/movie-covers/cover-40.png', 0, 1, 40),
(41, '花木兰（动画版）',     '动画/历史', 88,  8.1, '勇敢与担当', '代父从军的巾帼故事', '/movie-covers/cover-41.png', 0, 1, 41),
(42, '赛车总动员',           '动画/成长', 117, 8.4, '友谊与初心', '速度与激情之外的友情', '/movie-covers/cover-42.png', 1, 1, 42),
(43, '怪兽电力公司',         '动画/喜剧', 92,  8.7, '勇气与善意', '笑声是最强的能量', '/movie-covers/cover-43.png', 0, 1, 43),
(44, '勇敢传说',             '动画/奇幻', 93,  7.6, '亲子与勇敢', '修复母女关系，学会沟通', '/movie-covers/cover-44.png', 0, 1, 44),
(45, '美食总动员',           '动画/成长', 111, 8.4, '梦想与坚持', '人人皆可烹饪，巴黎屋顶厨房', '/movie-covers/cover-45.png', 0, 1, 45),
(46, '极地特快',             '动画/奇幻', 100, 7.9, '相信与童真', '相信圣诞老人的孩子才看得到', '/movie-covers/cover-46.png', 0, 1, 46),
(47, '虫虫特工队',           '动画/冒险', 95,  8.1, '团结与智慧', '小蚂蚁战胜大蚱蜢', '/movie-covers/cover-47.png', 1, 1, 47),
(48, '小门神',               '动画/神话', 107, 7.4, '传统与创新', '国产动画，门神下凡的故事', '/movie-covers/cover-48.png', 0, 1, 48),
(49, '魁拔',                 '动画/奇幻', 83,  8.4, '成长与使命', '国产奇幻，少年妖侠的成长', '/movie-covers/cover-49.png', 0, 1, 49),
(50, '西游记之大圣归来',     '动画/神话', 89,  8.4, '热血与救赎', '国产3D动画突破之作', '/movie-covers/cover-50.png', 1, 1, 50);
