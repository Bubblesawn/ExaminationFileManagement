import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import { getToken, hasPermission } from '../utils/authToken'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('../views/login/LoginView.vue') },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/dashboard/DashboardView.vue'), meta: { title: '工作台', permission: 'dashboard:view' } },
      { path: 'candidates', component: () => import('../views/candidate/CandidateListView.vue'), meta: { title: '考生管理', permission: 'candidate:view' } },
      { path: 'records', component: () => import('../views/record/RecordListView.vue'), meta: { title: '考籍档案', permission: 'record:view' } },
      { path: 'records/change-logs', component: () => import('../views/record/RecordChangeLogView.vue'), meta: { title: '档案变更记录', permission: 'record:view' } },
      { path: 'materials', component: () => import('../views/material/MaterialAuditView.vue'), meta: { title: '材料审核', permission: 'material:audit:view' } },
      { path: 'exemptions', component: () => import('../views/exemption/ExemptionView.vue'), meta: { title: '免考管理', permission: 'exemption:view' } },
      { path: 'courses', component: () => import('../views/course/CourseReplaceView.vue'), meta: { title: '课程顶替', permission: 'course-replace:view' } },
      { path: 'transfers', component: () => import('../views/transfer/TransferView.vue'), meta: { title: '转入转出', permission: 'transfer:view' } },
      { path: 'graduations', component: () => import('../views/graduation/GraduationView.vue'), meta: { title: '毕业管理', permission: 'graduation:view' } },
      { path: 'ai', component: () => import('../views/ai/AiAssistantView.vue'), meta: { title: '智能辅助', permission: 'ai:view' } },
      { path: 'system', component: () => import('../views/system/UserManageView.vue'), meta: { title: '用户管理', permission: 'system:user:view' } },
      { path: 'system/roles', component: () => import('../views/system/SystemView.vue'), meta: { title: '角色管理', permission: 'system:role:view' } },
      { path: 'system/menus', component: () => import('../views/system/MenuManageView.vue'), meta: { title: '菜单管理', permission: 'system:menu:view' } },
      { path: 'system/logs', component: () => import('../views/system/LogManageView.vue'), meta: { title: '日志管理', permission: 'system:log:view' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    return true
  }

  if (!getToken()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.path !== '/dashboard' && !hasPermission(to.meta.permission as string | undefined)) {
    return '/dashboard'
  }

  return true
})

export default router

