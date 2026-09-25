import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/api/http'
import ProfileSelect from '@/views/ProfileSelect.vue'
import PinLogin from '@/views/PinLogin.vue'
import GameMap from '@/views/GameMap.vue'
import GamePlay from '@/views/GamePlay.vue'
import ResultView from '@/views/ResultView.vue'
import Shop from '@/views/Shop.vue'
import Me from '@/views/Me.vue'

// 静态导入：兼容 iOS 11（不支持动态 import()）
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'profiles', component: ProfileSelect },
    { path: '/pin', name: 'pin', component: PinLogin },
    {
      path: '/map',
      name: 'map',
      component: GameMap,
      meta: { auth: true }
    },
    {
      path: '/play/:levelId',
      name: 'play',
      component: GamePlay,
      meta: { auth: true }
    },
    {
      path: '/result',
      name: 'result',
      component: ResultView,
      meta: { auth: true }
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
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.auth && !getToken()) {
    return { name: 'profiles' }
  }
  // 已登录再访问档案选择/PIN 页时直接进地图
  if (getToken() && (to.name === 'profiles' || to.name === 'pin')) {
    return { name: 'map' }
  }
  return true
})

export default router
