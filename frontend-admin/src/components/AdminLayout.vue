<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="brand">📚 闯关游戏 · 家长后台</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/"><el-icon><Odometer /></el-icon>仪表盘</el-menu-item>
        <el-menu-item index="/children"><el-icon><User /></el-icon>孩子管理</el-menu-item>
        <el-menu-item index="/catalog"><el-icon><Reading /></el-icon>单元与课文</el-menu-item>
        <el-menu-item index="/questions"><el-icon><EditPen /></el-icon>题库管理</el-menu-item>
        <el-menu-item index="/import"><el-icon><Upload /></el-icon>批量导入</el-menu-item>
        <el-menu-item index="/prizes"><el-icon><Present /></el-icon>奖品管理</el-menu-item>
        <el-menu-item index="/redeems">
          <el-icon><List /></el-icon>
          <template #title>
            兑换审核
            <el-badge v-if="pendingCount > 0" :value="pendingCount" class="badge" />
          </template>
        </el-menu-item>
        <el-menu-item index="/config"><el-icon><Setting /></el-icon>规则配置</el-menu-item>
        <el-menu-item index="/stats"><el-icon><DataAnalysis /></el-icon>数据统计</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div />
        <div class="header-right">
          <span class="admin-name">{{ auth.admin?.nickname || auth.admin?.username }}</span>
          <el-dropdown trigger="click" @command="onCommand">
            <el-button text>
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer, User, Reading, EditPen, Upload, Present, List,
  Setting, ArrowDown, DataAnalysis
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { adminApi } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/change-password')) return '/'
  return route.path
})

const pendingCount = ref(0)

async function loadPending() {
  try {
    const data = await adminApi.redeemList({ page: 1, size: 1, status: 'PENDING' })
    pendingCount.value = data.total
  } catch {
    // 静默：角标失败不影响主功能
  }
}

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    auth.logout()
    router.push({ name: 'login' })
  } else if (cmd === 'password') {
    router.push({ name: 'changePassword' })
  }
}

onMounted(loadPending)
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: #001529;
}

.brand {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  padding: 20px 16px;
}

.menu {
  border-right: none;
}

.badge {
  margin-left: 8px;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8eaf1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-name {
  color: #333;
  font-weight: 600;
}

.main {
  padding: 20px;
}
</style>
