export interface Movie {
  no: number
  name: string
  type: string
  duration: number
  rating: number
  theme: string
  note: string
  watched: boolean
  cover: string
}

export const movies: Movie[] = [
  { no: 1, name: '放牛班的春天', type: '成长/音乐', duration: 97, rating: 9.3, theme: '包容与赏识', note: '用合唱唤醒问题学生，比说教更动人', watched: true, cover: '/movie-covers/cover-1.png' },
  { no: 2, name: '海蒂和爷爷', type: '成长/亲情', duration: 111, rating: 9.3, theme: '乐观与真诚', note: '阿尔卑斯山风光治愈，教会孩子真诚待人', watched: true, cover: '/movie-covers/cover-2.png' },
  { no: 3, name: '寻梦环游记', type: '动画/亲情', duration: 105, rating: 9.1, theme: '亲情与梦想', note: '被遗忘才是真正的消失，画面唯美', watched: true, cover: '/movie-covers/cover-3.png' },
  { no: 4, name: '疯狂动物城', type: '动画/励志', duration: 109, rating: 9.2, theme: '梦想与坚持', note: '努力才能实现不可能，适合聊偏见与成见', watched: true, cover: '/movie-covers/cover-4.png' },
  { no: 5, name: '千与千寻', type: '动画/奇幻', duration: 125, rating: 9.4, theme: '勇气与成长', note: '宫崎骏经典，独立面对陌生世界', watched: true, cover: '/movie-covers/cover-5.png' },
  { no: 6, name: '龙猫', type: '动画/奇幻', duration: 86, rating: 9.2, theme: '童年与想象', note: '纯真童年与自然之美，画面温暖', watched: false, cover: '/movie-covers/cover-6.png' },
  { no: 7, name: '天空之城', type: '动画/冒险', duration: 124, rating: 9.2, theme: '友谊与冒险', note: '拉普达的冒险，关于守护与贪婪', watched: false, cover: '/movie-covers/cover-7.png' },
  { no: 8, name: '哈尔的移动城堡', type: '动画/奇幻', duration: 119, rating: 9.1, theme: '爱与接纳', note: '真正的美来自内心，反战主题温和', watched: true, cover: '/movie-covers/cover-8.png' },
  { no: 9, name: '奇迹男孩', type: '成长/校园', duration: 113, rating: 8.6, theme: '善良与包容', note: '选择善良，聊校园接纳特别合适', watched: true, cover: '/movie-covers/cover-9.png' },
  { no: 10, name: '头脑特工队', type: '动画/成长', duration: 95, rating: 8.8, theme: '情绪启蒙', note: '悲伤也重要，帮孩子疏导情绪', watched: false, cover: '/movie-covers/cover-10.png' },
  { no: 11, name: '飞屋环游记', type: '动画/冒险', duration: 96, rating: 9.1, theme: '爱与冒险', note: '开头十分钟就让人落泪，讲陪伴', watched: true, cover: '/movie-covers/cover-11.png' },
  { no: 12, name: '机器人总动员', type: '动画/科幻', duration: 98, rating: 9.3, theme: '环保与勇气', note: '几乎没有对白的浪漫科幻', watched: true, cover: '/movie-covers/cover-12.png' },
  { no: 13, name: '忠犬八公的故事', type: '剧情/动物', duration: 93, rating: 9.4, theme: '忠诚与陪伴', note: '催泪但温暖，理解什么是等待', watched: false, cover: '/movie-covers/cover-13.png' },
  { no: 14, name: '小鞋子', type: '剧情/家庭', duration: 89, rating: 9.2, theme: '亲情与担当', note: '贫寒兄妹的换鞋接力，珍惜当下', watched: true, cover: '/movie-covers/cover-14.png' },
  { no: 15, name: '地球上的星星', type: '成长/教育', duration: 165, rating: 8.9, theme: '发现天赋', note: '成绩不能定义一个人，老师视角', watched: true, cover: '/movie-covers/cover-15.png' },
  { no: 16, name: '当幸福来敲门', type: '剧情/励志', duration: 117, rating: 9.2, theme: '坚持与父爱', note: '逆境中的父爱，适合聊努力的意义', watched: false, cover: '/movie-covers/cover-16.png' },
  { no: 17, name: '美丽人生', type: '剧情/二战', duration: 116, rating: 9.6, theme: '父爱与乐观', note: '父亲用游戏保护孩子，需家长陪同看', watched: false, cover: '/movie-covers/cover-17.png' },
  { no: 18, name: '摔跤吧！爸爸', type: '体育/励志', duration: 140, rating: 9.0, theme: '拼搏与梦想', note: '女性力量，聊聊坚持与偏见', watched: true, cover: '/movie-covers/cover-18.png' },
  { no: 19, name: '哪吒之魔童降世', type: '动画/神话', duration: 110, rating: 8.4, theme: '打破偏见', note: '我命由我不由天，国漫之光', watched: true, cover: '/movie-covers/cover-19.png' },
  { no: 20, name: '雄狮少年', type: '动画/成长', duration: 104, rating: 8.4, theme: '热血与坚持', note: '舞狮少年的追梦路，国风励志', watched: false, cover: '/movie-covers/cover-20.png' },
  { no: 21, name: '长安三万里', type: '动画/历史', duration: 168, rating: 8.3, theme: '诗词与家国', note: '48首唐诗串起大唐，语文加分', watched: true, cover: '/movie-covers/cover-21.png' },
  { no: 22, name: '天书奇谭', type: '动画/神话', duration: 89, rating: 9.2, theme: '经典国漫', note: '国产美术片经典，故事生动幽默', watched: false, cover: '/movie-covers/cover-22.png' },
  { no: 23, name: '大闹天宫', type: '动画/神话', duration: 114, rating: 9.4, theme: '经典国漫', note: '上海美影厂经典，孙悟空不畏强权', watched: true, cover: '/movie-covers/cover-23.png' },
  { no: 24, name: '狮子王', type: '动画/成长', duration: 89, rating: 9.1, theme: '责任与成长', note: '哈姆雷特式的狮子故事，生生不息', watched: true, cover: '/movie-covers/cover-24.png' },
  { no: 25, name: '海底总动员', type: '动画/冒险', duration: 100, rating: 8.5, theme: '父爱与勇气', note: '寻找尼莫，聊恐惧与勇敢', watched: true, cover: '/movie-covers/cover-25.png' },
  { no: 26, name: '玩具总动员', type: '动画/友情', duration: 81, rating: 8.7, theme: '友情与告别', note: '陪伴与放手，系列都可看', watched: true, cover: '/movie-covers/cover-26.png' },
  { no: 27, name: '冰雪奇缘', type: '动画/奇幻', duration: 102, rating: 8.4, theme: '姐妹与自我', note: '做自己，经典歌曲朗朗上口', watched: true, cover: '/movie-covers/cover-27.png' },
  { no: 28, name: '超能陆战队', type: '动画/科幻', duration: 102, rating: 8.8, theme: '科技与温暖', note: '大白治愈，聊聊科技向善', watched: true, cover: '/movie-covers/cover-28.png' },
  { no: 29, name: '驯龙高手', type: '动画/冒险', duration: 98, rating: 8.8, theme: '理解与共情', note: '跨越偏见成为朋友', watched: true, cover: '/movie-covers/cover-29.png' },
  { no: 30, name: '神偷奶爸', type: '动画/喜剧', duration: 95, rating: 8.6, theme: '亲情与改变', note: '小黄人爆笑，坏蛋也能变好爸爸', watched: true, cover: '/movie-covers/cover-30.png' },
  { no: 31, name: '神隐少女', type: '动画/奇幻', duration: 125, rating: 9.4, theme: '成长与勇气', note: '即千与千寻，日本动画巅峰', watched: false, cover: '/movie-covers/cover-31.png' },
  { no: 32, name: '红猪', type: '动画/冒险', duration: 94, rating: 8.6, theme: '梦想与自由', note: '宫崎骏的中年浪漫，适合大孩子', watched: false, cover: '/movie-covers/cover-32.png' },
  { no: 33, name: '风之谷', type: '动画/科幻', duration: 116, rating: 8.9, theme: '环保与和平', note: '末日世界里的希望，反战反杀戮', watched: false, cover: '/movie-covers/cover-33.png' },
  { no: 34, name: '幽灵公主', type: '动画/奇幻', duration: 133, rating: 8.9, theme: '人与自然', note: '探讨人与自然的冲突，稍深刻', watched: false, cover: '/movie-covers/cover-34.png' },
  { no: 35, name: '借东西的小人阿莉埃蒂', type: '动画/奇幻', duration: 94, rating: 8.8, theme: '微小与勇敢', note: '借物一族的秘密生活', watched: false, cover: '/movie-covers/cover-35.png' },
  { no: 36, name: '侧耳倾听', type: '动画/青春', duration: 111, rating: 8.9, theme: '梦想与努力', note: '为梦想努力的青春故事', watched: false, cover: '/movie-covers/cover-36.png' },
  { no: 37, name: '悬崖上的金鱼姬', type: '动画/奇幻', duration: 101, rating: 8.6, theme: '纯真与承诺', note: '波妞与宗介的童真之约', watched: true, cover: '/movie-covers/cover-37.png' },
  { no: 38, name: '夏日友晴天', type: '动画/成长', duration: 95, rating: 8.4, theme: '友谊与接纳', note: '海怪男孩的人类世界冒险', watched: false, cover: '/movie-covers/cover-38.png' },
  { no: 39, name: '1/2的魔法', type: '动画/奇幻', duration: 102, rating: 7.9, theme: '亲情与成长', note: '一对兄弟的魔法冒险', watched: true, cover: '/movie-covers/cover-39.png' },
  { no: 40, name: '心灵奇旅', type: '动画/成长', duration: 100, rating: 8.7, theme: '热爱与生活', note: '好好感受当下的每一刻', watched: false, cover: '/movie-covers/cover-40.png' },
  { no: 41, name: '花木兰（动画版）', type: '动画/历史', duration: 88, rating: 8.1, theme: '勇敢与担当', note: '代父从军的巾帼故事', watched: false, cover: '/movie-covers/cover-41.png' },
  { no: 42, name: '赛车总动员', type: '动画/成长', duration: 117, rating: 8.4, theme: '友谊与初心', note: '速度与激情之外的友情', watched: true, cover: '/movie-covers/cover-42.png' },
  { no: 43, name: '怪兽电力公司', type: '动画/喜剧', duration: 92, rating: 8.7, theme: '勇气与善意', note: '笑声是最强的能量', watched: false, cover: '/movie-covers/cover-43.png' },
  { no: 44, name: '勇敢传说', type: '动画/奇幻', duration: 93, rating: 7.6, theme: '亲子与勇敢', note: '修复母女关系，学会沟通', watched: false, cover: '/movie-covers/cover-44.png' },
  { no: 45, name: '美食总动员', type: '动画/成长', duration: 111, rating: 8.4, theme: '梦想与坚持', note: '人人皆可烹饪，巴黎屋顶厨房', watched: false, cover: '/movie-covers/cover-45.png' },
  { no: 46, name: '极地特快', type: '动画/奇幻', duration: 100, rating: 7.9, theme: '相信与童真', note: '相信圣诞老人的孩子才看得到', watched: false, cover: '/movie-covers/cover-46.png' },
  { no: 47, name: '虫虫特工队', type: '动画/冒险', duration: 95, rating: 8.1, theme: '团结与智慧', note: '小蚂蚁战胜大蚱蜢', watched: true, cover: '/movie-covers/cover-47.png' },
  { no: 48, name: '小门神', type: '动画/神话', duration: 107, rating: 7.4, theme: '传统与创新', note: '国产动画，门神下凡的故事', watched: false, cover: '/movie-covers/cover-48.png' },
  { no: 49, name: '魁拔', type: '动画/奇幻', duration: 83, rating: 8.4, theme: '成长与使命', note: '国产奇幻，少年妖侠的成长', watched: false, cover: '/movie-covers/cover-49.png' },
  { no: 50, name: '西游记之大圣归来', type: '动画/神话', duration: 89, rating: 8.4, theme: '热血与救赎', note: '国产3D动画突破之作', watched: true, cover: '/movie-covers/cover-50.png' },
]
