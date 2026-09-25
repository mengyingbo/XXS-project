<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="title">📚 随堂知识闯关 · 家长后台</h2>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-alert
          v-if="errMsg"
          type="error"
          :title="errMsg"
          show-icon
          :closable="false"
          style="margin-bottom: 14px"
        />
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入管理员账号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errMsg = ref('')

async function submit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  errMsg.value = ''
  try {
    await auth.login(form.username.trim(), form.password)
    // mustChangePwd 由路由守卫自动导向改密页
    router.push({ name: 'dashboard' })
  } catch (e) {
    errMsg.value = (e as Error).message || '登录失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 12px 8px;
}

.title {
  text-align: center;
  margin: 8px 0 24px;
  color: #303133;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
