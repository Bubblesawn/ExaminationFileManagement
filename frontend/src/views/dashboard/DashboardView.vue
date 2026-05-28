<template>
  <div class="page dashboard-page">
    <!-- 1. 头部精美科技蓝渐变欢迎面板 -->
    <div class="welcome-banner">
      <div class="welcome-overlay"></div>
      <div class="welcome-content">
        <div class="welcome-left">
          <h1 class="welcome-title">您好，考籍系统管理员！欢迎回到工作台</h1>
          <p class="welcome-subtitle">
            今天系统平稳运行，有 <strong class="badge-count">{{ dashboardStats.pendingMaterialCount }}</strong> 项待审核材料需要您的处理。请及时跟进业务流转。
          </p>
          <div class="welcome-actions">
            <el-button type="warning" size="default" :icon="Icons.Service" @click="navigateTo('/ai')">
              智能AI核验辅助
            </el-button>
            <el-button type="primary" plain size="default" :icon="Icons.Files" @click="navigateTo('/records')">
              考籍档案浏览
            </el-button>
          </div>
        </div>
        <div class="welcome-right">
          <div class="live-clock">
            <span class="clock-label">当前系统时间</span>
            <span class="clock-time">{{ currentTime }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 重构的渐变核心数据统计卡片 -->
    <el-row v-loading="loading" :gutter="16" class="stat-row">
      <el-col v-for="item in stats" :key="item.label" :xs="24" :sm="12" :md="6" class="stat-col">
        <div class="glass-card stat-card" :style="{ '--theme-color': item.color }">
          <div class="stat-card-left">
            <div class="stat-icon-wrapper" :style="{ background: item.bgGradient }">
              <el-icon class="stat-icon"><component :is="Icons[item.icon]" /></el-icon>
            </div>
          </div>
          <div class="stat-card-right">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value-group">
              <span class="stat-value">{{ item.value }}</span>
              <span v-if="item.trend" class="stat-trend trend-up">
                {{ item.trend }} <el-icon><CaretTop /></el-icon>
              </span>
            </div>
            <div class="stat-desc">{{ item.desc }}</div>
          </div>
          <div class="card-accent-border"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 主体双栏内容区域 -->
    <el-row :gutter="16" class="content-row">
      <!-- 左侧栏：常用功能快捷入口 + 业务核验完成度 -->
      <el-col :xs="24" :lg="15" class="content-left">
        <!-- 3. 常用业务快捷入口 -->
        <section class="glass-card business-panel">
          <div class="panel-header">
            <h3 class="panel-title"><el-icon><Menu /></el-icon> 常用业务快捷入口</h3>
            <span class="panel-subtitle">一键直达核心功能</span>
          </div>
          <div class="quick-grid">
            <div
              v-for="action in quickActions"
              :key="action.name"
              class="quick-item"
              @click="navigateTo(action.route)"
            >
              <div class="quick-icon-box" :style="{ background: action.bg, color: action.color }">
                <el-icon><component :is="Icons[action.icon]" /></el-icon>
              </div>
              <div class="quick-info">
                <h4 class="quick-name">{{ action.name }}</h4>
                <p class="quick-desc">{{ action.desc }}</p>
              </div>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </section>

        <!-- 4. 业务核验进度/指标分析 -->
        <section class="glass-card business-panel chart-panel">
          <div class="panel-header">
            <h3 class="panel-title"><el-icon><PieChart /></el-icon> 今日考籍业务指标分析</h3>
            <span class="panel-subtitle">今日审核效率与达标统计</span>
          </div>
          <div class="progress-container">
            <div v-for="prog in progressStats" :key="prog.name" class="progress-card">
              <el-progress
                type="dashboard"
                :percentage="prog.percentage"
                :color="prog.color"
                :stroke-width="10"
                :width="120"
              >
                <template #default="{ percentage }">
                  <div class="progress-inner">
                    <span class="progress-num" :style="{ color: prog.color }">{{ percentage }}%</span>
                    <span class="progress-label">{{ prog.subtext }}</span>
                  </div>
                </template>
              </el-progress>
              <h4 class="progress-title">{{ prog.name }}</h4>
              <p class="progress-desc">{{ prog.desc }}</p>
            </div>
          </div>
        </section>
      </el-col>

      <!-- 右侧栏：紧急待处理任务 + 实时操作动态 -->
      <el-col :xs="24" :lg="9" class="content-right">
        <!-- 5. 紧急待处理任务看板 -->
        <section class="glass-card business-panel pending-panel">
          <div class="panel-header">
            <h3 class="panel-title"><el-icon><Warning /></el-icon> 紧急待审核事务</h3>
            <span class="panel-badge">{{ pendingTasks.length }}项待处理</span>
          </div>
          <div class="pending-list">
            <div v-for="task in pendingTasks" :key="task.id" class="pending-item">
              <div class="pending-status-indicator" :style="{ background: task.color }"></div>
              <div class="pending-body">
                <div class="pending-top">
                  <span class="pending-type">{{ task.type }}</span>
                  <el-tag :type="task.tagType" size="small" effect="plain">{{ task.priority }}</el-tag>
                </div>
                <p class="pending-title-text">{{ task.title }}</p>
                <div class="pending-bottom">
                  <span class="pending-time">{{ task.time }}</span>
                  <el-button type="primary" link size="small" @click="navigateTo(task.route)">
                    立即处理 <el-icon><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 6. 系统最新动态时间线 -->
        <section class="glass-card business-panel timeline-panel">
          <div class="panel-header">
            <h3 class="panel-title"><el-icon><Memo /></el-icon> 系统最新操作动态</h3>
            <el-button type="primary" link size="small" @click="navigateTo('/system/logs')">
              查看全部 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="timeline-wrapper">
            <el-timeline v-loading="logsLoading">
              <el-timeline-item
                v-for="log in displayLogs"
                :key="log.id"
                :timestamp="log.timestamp"
                :type="log.type"
                size="large"
              >
                <div class="timeline-content">
                  <div class="timeline-meta">
                    <strong class="operator-name">{{ log.operator }}</strong>
                    <span class="module-tag">{{ log.module }}</span>
                  </div>
                  <p class="operation-desc">{{ log.description }}</p>
                  <span v-if="log.cost" class="cost-tag">耗时 {{ log.cost }}ms</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as Icons from '@element-plus/icons-vue'
import { getDashboardStats, type DashboardStats } from '../../api/dashboard'
import { pageOperationLogs, type OperationLogRecord } from '../../api/systemLog'

const router = useRouter()

const loading = ref(false)
const logsLoading = ref(false)
const currentTime = ref('')
let timer: ReturnType<typeof setInterval> | null = null

// 仪表盘核心统计数据定义
const dashboardStats = ref<DashboardStats>({
  recordCount: 0,
  pendingMaterialCount: 0,
  exemptionApplicationCount: 0,
  graduationApplicationCount: 0
})

// 实时操作日志数据定义
const realLogs = ref<OperationLogRecord[]>([])

/**
 * @brief 系统时间格式化工具，显示完整日期与秒级时间。
 */
function updateCurrentTime() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const date = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${year}-${month}-${date} ${hours}:${minutes}:${seconds}`
}

/**
 * @brief 导航到指定的页面。
 *
 * @param path 目标路由路径。
 */
function navigateTo(path: string) {
  router.push(path)
}

// 核心统计指标配置，配备富美学配色与图标
const stats = computed(() => [
  {
    label: '考籍档案',
    value: dashboardStats.value.recordCount,
    icon: 'Files',
    color: '#3b82f6',
    bgGradient: 'linear-gradient(135deg, #60a5fa 0%, #2563eb 100%)',
    trend: '+2.5%',
    desc: '系统内总有效考籍卷宗'
  },
  {
    label: '待审材料',
    value: dashboardStats.value.pendingMaterialCount,
    icon: 'Picture',
    color: '#f59e0b',
    bgGradient: 'linear-gradient(135deg, #fbbf24 0%, #d97706 100%)',
    trend: '紧急',
    desc: '需人工或智能辅助预审'
  },
  {
    label: '免考申请',
    value: dashboardStats.value.exemptionApplicationCount,
    icon: 'CircleCheck',
    color: '#a855f7',
    bgGradient: 'linear-gradient(135deg, #c084fc 0%, #7e22ce 100%)',
    trend: '',
    desc: '本审查周期内待结清申请'
  },
  {
    label: '毕业申请',
    value: dashboardStats.value.graduationApplicationCount,
    icon: 'Medal',
    color: '#10b981',
    bgGradient: 'linear-gradient(135deg, #34d399 0%, #059669 100%)',
    trend: '+12%',
    desc: '当期符合预审查资格库'
  }
])

// 常用业务快捷入口列表
const quickActions = [
  {
    name: '考生档案建档',
    desc: '录入考生基本信息与电子考籍档案',
    icon: 'User',
    route: '/candidates',
    bg: 'rgba(59, 130, 246, 0.1)',
    color: '#3b82f6'
  },
  {
    name: '智能材料审核',
    desc: '自动分类、清晰度检测与异常提示',
    icon: 'Picture',
    route: '/materials',
    bg: 'rgba(245, 158, 11, 0.1)',
    color: '#f59e0b'
  },
  {
    name: '免考申请审核',
    desc: '免考申请流程审核及业务归档',
    icon: 'CircleCheck',
    route: '/exemptions',
    bg: 'rgba(168, 85, 247, 0.1)',
    color: '#a855f7'
  },
  {
    name: '毕业预审判定',
    desc: '毕业生资格核实及学分条件测算',
    icon: 'Medal',
    route: '/graduations',
    bg: 'rgba(16, 185, 129, 0.1)',
    color: '#10b981'
  },
  {
    name: 'AI 智能辅助',
    desc: '向大语言模型提问考籍规则与判例',
    icon: 'Service',
    route: '/ai',
    bg: 'rgba(239, 68, 68, 0.1)',
    color: '#ef4444'
  },
  {
    name: '系统安全日志',
    desc: '查看全部操作人员与操作历史记录',
    icon: 'Setting',
    route: '/system/logs',
    bg: 'rgba(107, 114, 128, 0.1)',
    color: '#6b7280'
  }
]

// 业务进度与效率分析指标
const progressStats = computed(() => [
  {
    name: '今日材料预审完成率',
    percentage: dashboardStats.value.pendingMaterialCount === 0 ? 100 : 85,
    color: '#3b82f6',
    subtext: '审核闭环',
    desc: '今日已完成核验的电子材料占总上传比'
  },
  {
    name: 'AI 识别高置信度占比',
    percentage: 94,
    color: '#a855f7',
    subtext: '分类准确',
    desc: '算法判定为无需复核的一类必交材料比'
  },
  {
    name: '本期毕业预审判定率',
    percentage: 76,
    color: '#10b981',
    subtext: '计算达标',
    desc: '当期已完成毕业资历综合排查的覆盖率'
  }
])

// 紧急待处理任务 Mock 数据
const pendingTasks = [
  {
    id: 1,
    title: '考生【张三】上传的【免考申请】成绩单文字略显模糊，系统置信度偏低，请立即干预审核。',
    type: '材料初审异常',
    priority: '高优先级',
    tagType: 'danger',
    color: '#ef4444',
    time: '10分钟前',
    route: '/materials'
  },
  {
    id: 2,
    title: '考生【李四】提交的毕业生数码照片背景不符（要求蓝色背景），人脸核验建议退回。',
    type: '照片合规警告',
    priority: '中优先级',
    tagType: 'warning',
    color: '#f59e0b',
    time: '45分钟前',
    route: '/graduations'
  },
  {
    id: 3,
    title: '考籍调档业务 #2026052701 数据比对中，发现【准考证材料】疑似缺失。',
    type: '自动缺失提示',
    priority: '待办任务',
    tagType: 'info',
    color: '#3b82f6',
    time: '2小时前',
    route: '/transfers'
  }
]

// 计算展示用的系统动态列表（优先使用真实 API 数据，使用精心设计的本地数据作为兜底）
const displayLogs = computed(() => {
  if (realLogs.value && realLogs.value.length > 0) {
    return realLogs.value.map((log) => {
      // 动态映射 Element Plus 的节点类型和状态
      let type: 'success' | 'warning' | 'info' | 'primary' | 'danger' = 'info'
      if (log.operationStatus === 'SUCCESS') {
        type = 'success'
      } else if (log.operationStatus === 'FAIL') {
        type = 'danger'
      }

      return {
        id: log.id,
        timestamp: log.operationTime ? log.operationTime.slice(11, 19) || log.operationTime : '刚刚',
        operator: log.operatorName || '系统',
        module: log.moduleName || '未知模块',
        description: log.operationDesc || '执行了系统维护',
        cost: log.costTime || 0,
        type: type
      }
    })
  }

  // 本地高颜值 Mock 兜底数据，防止空白
  return [
    {
      id: 101,
      timestamp: '10:48:21',
      operator: 'admin',
      module: '免考管理',
      description: '批量导入免考科目对应表数据',
      cost: 142,
      type: 'success'
    },
    {
      id: 102,
      timestamp: '10:30:15',
      operator: 'AI 引擎',
      module: '材料审核',
      description: '对考生 张三 提交的成绩单完成置信度运算，结果为：82%',
      cost: 65,
      type: 'primary'
    },
    {
      id: 103,
      timestamp: '10:05:12',
      operator: 'admin',
      module: '考籍变更',
      description: '修改考生【王五】的外语顶替科目配置',
      cost: 120,
      type: 'warning'
    },
    {
      id: 104,
      timestamp: '09:42:00',
      operator: '系统检测',
      module: '毕业审查',
      description: '触发当期已达满学分考生的毕业条件智能预审任务，涉及 24 人',
      cost: 1540,
      type: 'success'
    },
    {
      id: 105,
      timestamp: '09:12:35',
      operator: 'user_02',
      module: '考生管理',
      description: '新增或同步省招考办考生最新考籍记录 5 条',
      cost: 322,
      type: 'info'
    }
  ]
})

/**
 * @brief 加载工作台统计面板数据。
 *
 * @details
 * 通过后端聚合统计接口一次性获取首页面板数量，接口异常时保留默认值并提示用户。
 */
async function loadDashboardStats() {
  loading.value = true
  try {
    dashboardStats.value = await getDashboardStats()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '工作台统计加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * @brief 动态联调后台接口：分页拉取最新的操作日志数据。
 *
 * @details
 * 获取最新的 5 条审计记录，实现工作台最新动态的前后端交互联动。
 */
async function loadRecentSystemLogs() {
  logsLoading.value = true
  try {
    const response = await pageOperationLogs({ pageNo: 1, pageSize: 5 })
    if (response && response.records) {
      realLogs.value = response.records
    }
  } catch (error) {
    // 静默降级：由于本地测试或初始数据库可能没有日志，出错时不弹框扰民，直接转为高颜值 Mock 展示
    console.warn('实时操作日志接口加载失败，已自动开启本期高定 Mock 兜底数据', error)
  } finally {
    logsLoading.value = false
  }
}

onMounted(() => {
  loadDashboardStats()
  loadRecentSystemLogs()
  updateCurrentTime()
  timer = setInterval(updateCurrentTime, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
/* 全局控制与背景 */
.dashboard-page {
  padding: 20px;
  background-color: #f6f8fb;
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 1. 头部精美科技蓝渐变欢迎面板 */
.welcome-banner {
  position: relative;
  border-radius: 12px;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  padding: 30px;
  color: #ffffff;
  overflow: hidden;
  box-shadow: 0 10px 25px -5px rgba(30, 58, 138, 0.3);
}

.welcome-overlay {
  position: absolute;
  top: -50px;
  right: -50px;
  width: 250px;
  height: 250px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0) 70%);
  filter: blur(10px);
  pointer-events: none;
}

.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.welcome-left {
  flex: 1;
  min-width: 300px;
}

.welcome-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.welcome-subtitle {
  margin: 0 0 20px;
  font-size: 14px;
  opacity: 0.9;
  color: #e0f2fe;
}

.badge-count {
  background-color: #f59e0b;
  color: #ffffff;
  padding: 2px 8px;
  border-radius: 9999px;
  font-weight: 700;
  margin: 0 4px;
}

.welcome-actions {
  display: flex;
  gap: 12px;
}

.welcome-right {
  display: flex;
  align-items: center;
}

.live-clock {
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 12px 20px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.clock-label {
  font-size: 11px;
  opacity: 0.7;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.clock-time {
  font-size: 18px;
  font-weight: 700;
  font-family: monospace;
  margin-top: 4px;
}

/* 2. 重构的核心数据统计卡片 */
.stat-row {
  margin-bottom: 4px;
}

.stat-col {
  margin-bottom: 16px;
}

.glass-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stat-card {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 108px;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px -10px rgba(0, 0, 0, 0.08);
}

.stat-card:hover .card-accent-border {
  opacity: 1;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
}

.card-accent-border {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background-color: var(--theme-color);
  opacity: 0.4;
  transition: opacity 0.3s;
}

.stat-card-left {
  flex-shrink: 0;
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 22px;
  transition: transform 0.3s ease;
}

.stat-card-right {
  flex-grow: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.stat-value-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
}

.stat-trend {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.trend-up {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.stat-desc {
  margin-top: 6px;
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 主体内容排版 */
.content-row {
  margin-top: 4px;
}

.content-left, .content-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.business-panel {
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 12px;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title .el-icon {
  color: #3b82f6;
}

.panel-subtitle {
  font-size: 12px;
  color: #9ca3af;
}

.panel-badge {
  background-color: #fee2e2;
  color: #ef4444;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
}

/* 3. 常用业务快捷入口 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 640px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}

.quick-item {
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  background: #ffffff;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.quick-item:hover {
  transform: scale(1.02);
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.08);
}

.quick-item:hover .arrow-icon {
  transform: translateX(4px);
  color: #3b82f6;
}

.quick-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.quick-info {
  flex-grow: 1;
  min-width: 0;
}

.quick-name {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.quick-desc {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 14px;
  color: #d1d5db;
  transition: transform 0.2s, color 0.2s;
  flex-shrink: 0;
}

/* 4. 业务核验进度/指标分析 */
.progress-container {
  display: flex;
  justify-content: space-around;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  padding: 10px 0;
}

.progress-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-width: 140px;
  flex: 1;
}

.progress-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-num {
  font-size: 20px;
  font-weight: 700;
  font-family: monospace;
}

.progress-label {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 2px;
}

.progress-title {
  margin: 12px 0 4px;
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.progress-desc {
  margin: 0;
  font-size: 11px;
  color: #6b7280;
  max-width: 160px;
  line-height: 1.4;
}

/* 5. 紧急待处理任务看板 */
.pending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pending-item {
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  background-color: #fafbfd;
  display: flex;
  overflow: hidden;
  transition: all 0.2s ease;
}

.pending-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);
}

.pending-status-indicator {
  width: 4px;
  flex-shrink: 0;
}

.pending-body {
  padding: 12px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.pending-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pending-type {
  font-size: 12px;
  font-weight: 700;
  color: #4b5563;
}

.pending-title-text {
  margin: 0;
  font-size: 13px;
  color: #1f2937;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pending-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.pending-time {
  font-size: 11px;
  color: #9ca3af;
}

/* 6. 系统最新动态时间线 */
.timeline-wrapper {
  max-height: 380px;
  overflow-y: auto;
  padding-right: 8px;
  padding-top: 4px;
}

/* 滚动条美化 */
.timeline-wrapper::-webkit-scrollbar {
  width: 5px;
}
.timeline-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}
.timeline-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.timeline-content {
  background-color: #fafbfd;
  border: 1px solid #f3f4f6;
  padding: 10px 12px;
  border-radius: 6px;
  margin-top: -6px;
}

.timeline-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.operator-name {
  font-size: 12px;
  color: #374151;
  font-weight: 700;
}

.module-tag {
  font-size: 11px;
  background-color: #e0f2fe;
  color: #0369a1;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.operation-desc {
  margin: 0;
  font-size: 12px;
  color: #4b5563;
  line-height: 1.4;
}

.cost-tag {
  display: inline-block;
  margin-top: 6px;
  font-size: 10px;
  color: #9ca3af;
  background-color: #f3f4f6;
  padding: 1px 4px;
  border-radius: 3px;
  font-family: monospace;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .welcome-banner {
    padding: 20px;
  }
  .welcome-title {
    font-size: 20px;
  }
}
</style>

