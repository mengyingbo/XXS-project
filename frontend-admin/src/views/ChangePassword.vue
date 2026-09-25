<template>
  <el-card class="pwd-card">
    <template #header>
      <span>修改密码</span>
    </template>
    <el-alert
      v-if="auth.mustChangePwd()"
      type="warning"
      show-icon
      :closable="false"
      title="首次登录请先修改默认密码，修改后才能使用其他功能"
      style="margin-bottom: 20px"
    />
    <el-form :model="form" label-width="90px" style="max-width: 420px">
      <el-form-item label="原密码">
        <el-input v-model="form.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6~30 位" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="form.confirm" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        <el-button v-if="!auth.mustChangePwd()" @click="router.back()">返回</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const saving = ref(false)

async function submit() {
  if (!form.oldPassword || !form.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (form.newPassword.length < 6 || form.newPassword.length > 30) {
    ElMessage.warning('新密码长度需在 6~30 位之间')
    return
  }
  if (form.newPassword !== form.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await adminApi.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    auth.markPwdChanged()
    ElMessage.success('密码修改成功')
    router.push({ name: 'dashboard' })
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.pwd-card {
  max-width: 640px;
}
</style>
