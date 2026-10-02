import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/api/http'
import { useChildStore } from '@/stores/child'
import ProfileSelect from '@/views/ProfileSelect.vue'
import PinLogin from '@/views/PinLogin.vue'
import SubjectSelect from '@/views/SubjectSelect.vue'
import GameMap from '@/views/GameMap.vue'
import GamePlay from '@/views/GamePlay.vue'
import ResultView from '@/views/ResultView.vue'
import Shop from '@/views/Shop.vue'
import Me from '@/views/Me.vue'
import GameHub from '@/views/games/GameHub.vue'
import Klotski from '@/views/games/Klotski.vue'
import Puzzle from '@/views/games/Puzzle.vue'
import Snake from '@/views/games/Snake.vue'
import Sudoku from '@/views/games/Sudoku.vue'
import IdiomGame from '@/views/games/IdiomGame.vue'

// 静态导入：兼容 iOS 11（不支持动态 import()）
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'profiles', component: ProfileSelect },
    { path: '/pin', name: 'pin', component: PinLogin },
    {
      path: '/subject',
      name: 'subject',
      component: SubjectSelect,
      meta: { auth: true }
    },
    {
      path: '/map',
      name: 'map',
      component: GameMap,
      // 需要先选科目（v2.0）
      meta: { auth: true, subject: true }
    },
    {
      path: '/play/:levelId',
      name: 'play',
      component: GamePlay,
      meta: { auth: true, subject: true }
    },
    {
      path: '/result',
      name: 'result',
      component: ResultView,
      meta: { auth: true, subject: true }
    },
    {
      path: '/shop',
      name: 'shop',
      component: Shop,
      meta: { auth: true }
    },
    {
      path: '/me',
      name: 'me',
      component: Me,
      meta: { auth: true }
    },
    /* ---- 游戏乐园（v2.1）：免登录，与闯关体系完全独立 ---- */
    { path: '/games', name: 'games', component: GameHub },
    { path: '/games/klotski', name: 'games-klotski', component: Klotski },
    { path: '/games/puzzle', name: 'games-puzzle', component: Puzzle },
    { path: '/games/snake', name: 'games-snake', component: Snake },
    { path: '/games/sudoku', name: 'games-sudoku', component: Sudoku },
    { path: '/games/idiom', name: 'games-idiom', component: IdiomGame }
  ]
})

router.beforeEach((to) => {
  if (to.meta.auth && !getToken()) {
    return { name: 'profiles' }
  }
  if (getToken()) {
    // 已登录但还没选科目：闯关相关页面一律先去科目选择页
    const store = useChildStore()
    if (to.meta.subject && !store.subject) {
      return { name: 'subject' }
    }
    // 已登录再访问档案选择/PIN 页时：有科目进地图，没有则先选科目
    if (to.name === 'profiles' || to.name === 'pin') {
      return store.subject ? { name: 'map' } : { name: 'subject' }
    }
  }
  return true
})

export default router
