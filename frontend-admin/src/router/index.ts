import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import Login from '@/views/Login.vue'
import AdminLayout from '@/components/AdminLayout.vue'
import Dashboard from '@/views/Dashboard.vue'
import ChangePassword from '@/views/ChangePassword.vue'
import Children from '@/views/Children.vue'
import Catalog from '@/views/Catalog.vue'
import Questions from '@/views/Questions.vue'
import QuestionImport from '@/views/QuestionImport.vue'
import Prizes from '@/views/Prizes.vue'
import Redeems from '@/views/Redeems.vue'
import ConfigView from '@/views/ConfigView.vue'
import StatsView from '@/views/StatsView.vue'

// 静态导入：兼容 iOS 11（不支持动态 import()）
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', name: 'login', component: Login },
    {
      path: '/',
      component: AdminLayout,
      meta: { auth: true },
      children: [
        { path: '', name: 'dashboard', component: Dashboard },
        { path: 'change-password', name: 'changePassword', component: ChangePassword },
        { path: 'children', name: 'children', component: Children },
        { path: 'catalog', name: 'catalog', component: Catalog },
        { path: 'questions', name: 'questions', component: Questions },
        { path: 'import', name: 'import', component: QuestionImport },
        { path: 'prizes', name: 'prizes', component: Prizes },
        { path: 'redeems', name: 'redeems', component: Redeems },
        { path: 'config', name: 'config', component: ConfigView },
        { path: 'stats', name: 'stats', component: StatsView }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// F-AD-01：首次登录（mustChangePwd）强制先改密
router.beforeEach((to) => {
  const auth = useAuthStore()
  const loggedIn = Boolean(getToken())

  const inLayout = to.matched.some((r) => r.path === '/')
  if (inLayout && !loggedIn) {
    return { name: 'login' }
  }
  if (loggedIn && to.name === 'login') {
    return { name: 'dashboard' }
  }
  if (inLayout && loggedIn && auth.mustChangePwd() && to.name !== 'changePassword') {
    return { name: 'changePassword' }
  }
  if (inLayout && loggedIn && !auth.mustChangePwd() && to.name === 'changePassword') {
    return { name: 'dashboard' }
  }
  return true
})

export default router
