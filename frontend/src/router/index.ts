import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import { getToken } from '../utils/authToken'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('../views/login/LoginView.vue') },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/dashboard/DashboardView.vue'), meta: { title: '工作台' } },
      { path: 'candidates', component: () => import('../views/candidate/CandidateListView.vue'), meta: { title: '考生管理' } },
      { path: 'records', component: () => import('../views/record/RecordListView.vue'), meta: { title: '考籍档案' } },
      { path: 'records/change-logs', component: () => import('../views/record/RecordChangeLogView.vue'), meta: { title: '档案变更记录' } },
      { path: 'materials', component: () => import('../views/material/MaterialAuditView.vue'), meta: { title: '材料审核' } },
      { path: 'exemptions', component: () => import('../views/exemption/ExemptionView.vue'), meta: { title: '免考管理' } },
      { path: 'courses', component: () => import('../views/course/CourseReplaceView.vue'), meta: { title: '课程顶替' } },
      { path: 'transfers', component: () => import('../views/transfer/TransferView.vue'), meta: { title: '转入转出' } },
      { path: 'graduations', component: () => import('../views/graduation/GraduationView.vue'), meta: { title: '毕业管理' } },
      { path: 'ai', component: () => import('../views/ai/AiAssistantView.vue'), meta: { title: '智能辅助' } },
      { path: 'system', component: () => import('../views/system/SystemView.vue'), meta: { title: '用户管理' } },
      { path: 'system/menus', component: () => import('../views/system/MenuManageView.vue'), meta: { title: '菜单管理' } },
      { path: 'system/logs', component: () => import('../views/system/LogManageView.vue'), meta: { title: '日志管理' } }
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

  return true
})

export default router

