<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>省考试院自学考试考籍管理系统</h1>
      <p class="login-subtitle">统一身份认证入口</p>
      <el-alert v-if="loginError" class="login-error" :title="loginError" type="error" show-icon :closable="false" />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="账号" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入账号" autocomplete="username" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
            clearable
          />
        </el-form-item>
        <el-button type="primary" size="large" :loading="submitting" @click="handleLogin">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../../api/auth'
import { saveLoginResult } from '../../utils/authToken'

interface LoginForm {
  username: string
  password: string
}

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const loginError = ref('')

const form = reactive<LoginForm>({
  username: '',
  password: ''
})

const rules: FormRules<LoginForm> = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度需为 3-32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需为 6-64 个字符', trigger: 'blur' }
  ]
}

/**
 * @brief 校验登录表单并提交登录请求。
 *
 * @details 登录成功后保存 Token，再跳转到原目标页面或工作台；登录失败时在页面内展示后端提示。
 */
async function handleLogin() {
  if (submitting.value) {
    return
  }

  loginError.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const loginResult = await login({
      username: form.username,
      password: form.password
    })
    saveLoginResult(loginResult)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    loginError.value = error instanceof Error ? error.message : '登录失败，请检查账号和密码'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 18% 20%, rgb(56 189 248 / 18%), transparent 32%),
    linear-gradient(135deg, #12213a 0%, #24516f 48%, #eef3f8 48%, #eef3f8 100%);
}

.login-panel {
  width: min(420px, calc(100vw - 32px));
  padding: 32px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 60px rgb(13 26 51 / 22%);
}

h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.35;
}

.login-subtitle {
  margin: 8px 0 24px;
  color: #64748b;
}

.login-error {
  margin-bottom: 18px;
}

.el-button {
  width: 100%;
  margin-top: 4px;
}
</style>
