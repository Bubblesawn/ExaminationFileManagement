<template>
  <el-container class="main-layout">
    <el-aside width="232px" class="sidebar">
      <div class="brand">考籍管理系统</div>
      <el-menu router :default-active="$route.path" background-color="#162033" text-color="#dbe4f0" active-text-color="#ffffff">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon><span>工作台</span></el-menu-item>
        <el-menu-item index="/candidates"><el-icon><User /></el-icon><span>考生管理</span></el-menu-item>
        <el-menu-item index="/records"><el-icon><Files /></el-icon><span>考籍档案</span></el-menu-item>
        <el-menu-item index="/materials"><el-icon><Picture /></el-icon><span>材料审核</span></el-menu-item>
        <el-menu-item index="/exemptions"><el-icon><CircleCheck /></el-icon><span>免考管理</span></el-menu-item>
        <el-menu-item index="/courses"><el-icon><Switch /></el-icon><span>课程顶替</span></el-menu-item>
        <el-menu-item index="/transfers"><el-icon><Sort /></el-icon><span>转入转出</span></el-menu-item>
        <el-menu-item index="/graduations"><el-icon><Medal /></el-icon><span>毕业管理</span></el-menu-item>
        <el-menu-item index="/ai"><el-icon><Service /></el-icon><span>智能辅助</span></el-menu-item>
        <el-menu-item index="/system"><el-icon><Setting /></el-icon><span>系统管理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <strong>{{ $route.meta.title }}</strong>
        <el-button type="primary" plain @click="$router.push('/login')">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { CircleCheck, DataBoard, Files, Medal, Picture, Service, Setting, Sort, Switch, User } from '@element-plus/icons-vue'
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.sidebar {
  background: #162033;
}

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
</style>

