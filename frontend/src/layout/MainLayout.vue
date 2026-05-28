<template>
  <el-container class="main-layout">
    <el-aside width="232px" class="sidebar">
      <div class="brand">考籍管理系统</div>
      <el-menu router :default-active="$route.path" background-color="#162033" text-color="#dbe4f0" active-text-color="#ffffff">
        <el-menu-item v-if="can('dashboard:view')" index="/dashboard"><el-icon><DataBoard /></el-icon><span>工作台</span></el-menu-item>
        <el-menu-item v-if="can('candidate:view')" index="/candidates"><el-icon><User /></el-icon><span>考生管理</span></el-menu-item>
        <el-sub-menu v-if="can('record:view')" index="/record-manage">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>考籍业务</span>
          </template>
          <el-menu-item index="/records"><span>考籍档案</span></el-menu-item>
          <el-menu-item index="/records/change-logs"><span>变更记录</span></el-menu-item>
        </el-sub-menu>
        <el-menu-item v-if="can('material:audit:view')" index="/materials"><el-icon><Picture /></el-icon><span>材料审核</span></el-menu-item>
        <el-menu-item v-if="can('exemption:view')" index="/exemptions"><el-icon><CircleCheck /></el-icon><span>免考管理</span></el-menu-item>
        <el-menu-item v-if="can('course-replace:view')" index="/courses"><el-icon><Switch /></el-icon><span>课程顶替</span></el-menu-item>
        <el-menu-item v-if="can('transfer:view')" index="/transfers"><el-icon><Sort /></el-icon><span>转入转出</span></el-menu-item>
        <el-menu-item v-if="can('graduation:view')" index="/graduations"><el-icon><Medal /></el-icon><span>毕业管理</span></el-menu-item>
        <el-menu-item v-if="can('ai:view')" index="/ai"><el-icon><Service /></el-icon><span>智能辅助</span></el-menu-item>
        <el-sub-menu v-if="hasSystemMenu" index="/system-manage">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-if="can('system:user:view')" index="/system"><span>用户管理</span></el-menu-item>
          <el-menu-item v-if="can('system:menu:view')" index="/system/menus"><span>菜单管理</span></el-menu-item>
          <el-menu-item v-if="can('system:log:view')" index="/system/logs"><span>日志管理</span></el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <strong>{{ $route.meta.title }}</strong>
        <el-button type="primary" plain @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { CircleCheck, DataBoard, Files, Medal, Picture, Service, Setting, Sort, Switch, User } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clearLoginResult, hasPermission } from '../utils/authToken'

const router = useRouter()
const systemPermissions = ['system:user:view', 'system:menu:view', 'system:log:view']
const hasSystemMenu = computed(() => systemPermissions.some((permission) => can(permission)))

/**
 * @brief 判断当前登录用户是否可查看指定菜单。
 *
 * @param permission 菜单权限码。
 * @return 是否拥有该菜单权限。
 */
function can(permission: string) {
  return hasPermission(permission)
}

/**
 * @brief 清理本地登录状态并返回登录页。
 */
function handleLogout() {
  clearLoginResult()
  router.replace('/login')
}
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
