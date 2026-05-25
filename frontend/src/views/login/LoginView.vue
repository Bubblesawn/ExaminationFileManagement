<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>省考试院自学考试考籍管理系统</h1>
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" size="large" @click="$router.push('/dashboard')">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

const form = reactive({
  username: 'admin',
  password: '123456'
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #27456d, #5d7fbf);
}

.login-panel {
  width: min(420px, calc(100vw - 32px));
  padding: 32px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 60px rgb(13 26 51 / 22%);
}

h1 {
  margin: 0 0 24px;
  font-size: 24px;
}

.el-button {
  width: 100%;
}
</style>

