<script setup>
import { ref, computed } from 'vue'
import AppTabBar from '../components/AppTabBar.vue'

// ===== Mock 数据（零使用状态）=====
const processingStatus = ref('pending')

const stats = ref({
  resumeCount: 0,
  aiUsageCount: 0,
  lastEditedDate: null,
  hasMasterResume: false,
})

const masterResume = ref(null)
const tailoredResumes = ref([])

// ===== 新建分组弹窗状态 =====
const showGroupModal = ref(false)
const newGroupName = ref('')
const groupNameError = ref('')
const MAX_GROUP_NAME_LENGTH = 20

function openGroupModal() {
  newGroupName.value = ''
  groupNameError.value = ''
  showGroupModal.value = true
}

function closeGroupModal() {
  showGroupModal.value = false
  newGroupName.value = ''
  groupNameError.value = ''
}

function handleCreateGroup() {
  const name = newGroupName.value.trim()

  if (!name) {
    groupNameError.value = '分组名称不能为空'
    return
  }

  if (name.length > MAX_GROUP_NAME_LENGTH) {
    groupNameError.value = `分组名称不得超过${MAX_GROUP_NAME_LENGTH}个字符`
    return
  }

  const newGroup = {
    groupId: `group-${Date.now()}`,
    groupName: name,
    count: 0,
    resumes: [],
  }

  resumeGroups.value.push(newGroup)
  closeGroupModal()
}

function handleGroupInput() {
  if (groupNameError.value) {
    groupNameError.value = ''
  }
}

// ===== 简历分组 Mock 数据（仅用于 UI 展示）=====
const resumeGroups = ref([
  {
    groupId: 'group-java',
    groupName: 'Java工程师简历',
    count: 2,
    resumes: [
      {
        id: 'java-1',
        title: 'Java高级开发_张三',
        snippet: '8年Java后端开发经验，熟悉Spring Cloud、微服务架构与中间件优化',
        initial: 'J',
        time: '2023-10-24',
        score: 85,
      },
      {
        id: 'java-2',
        title: '互联网大厂专用版',
        snippet: '针对互联网大厂JD定制，突出高并发与分布式系统设计能力',
        initial: 'I',
        time: '2023-10-20',
        score: 92,
      },
    ],
  },
  {
    groupId: 'group-cpp',
    groupName: 'C++工程师简历',
    count: 1,
    resumes: [
      {
        id: 'cpp-1',
        title: 'C++系统开发_李四',
        snippet: '5年C++系统开发经验，擅长高性能计算、网络编程与跨平台开发',
        initial: 'C',
        time: '2023-10-18',
        score: 88,
      },
    ],
  },
])

// ===== 工具函数 =====
const formatDate = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('zh-CN', { month: 'short', day: '2-digit', year: 'numeric' })
}

const getRelativeTime = (value) => {
  if (!value) return '暂无'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '暂无'
  const diff = Date.now() - date.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return formatDate(value)
}

const getMonogram = (title) => {
  const words = title.split(/\s+/).filter((w) => /^[a-zA-Z一-鿿]/.test(w))
  if (words.length === 0 && title.length > 0) return title.charAt(0).toUpperCase()
  return words.slice(0, 2).map((w) => w.charAt(0).toUpperCase()).join('')
}

const cardColors = [
  { bg: '#EEC3CF', fg: '#171717' },
  { bg: '#BCC99C', fg: '#171717' },
  { bg: '#BED2E4', fg: '#171717' },
  { bg: '#EAD574', fg: '#171717' },
  { bg: '#F1DCDD', fg: '#171717' },
  { bg: '#DCE3C8', fg: '#171717' },
  { bg: '#CFDDEA', fg: '#171717' },
  { bg: '#F2E7BF', fg: '#171717' },
]

const hashTitle = (title) => {
  let hash = 0
  for (let i = 0; i < title.length; i++) {
    hash = (hash << 5) - hash + title.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

const statusDisplay = computed(() => {
  switch (processingStatus.value) {
    case 'loading': return { text: '检查中...', color: '#64748b' }
    case 'processing': return { text: 'AI 解析中', color: '#2563eb' }
    case 'ready': return { text: '已就绪', color: '#10b981' }
    case 'failed': return { text: '处理失败', color: '#dc2626' }
    default: return { text: '等待上传', color: '#64748b' }
  }
})

const lastAiGenTime = ref(localStorage.getItem('last_ai_gen_time') || null)

const lastAiGenDisplay = computed(() => {
  if (!lastAiGenTime.value) return '从未'
  const diff = Date.now() - Number(lastAiGenTime.value)
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  return `${Math.floor(hours / 24)} 天前`
})

// ===== 简历分组计算属性 =====
const groupedResumes = computed(() => {
  // 优先使用 Mock 分组数据展示，原有数据逻辑保留
  if (resumeGroups.value && resumeGroups.value.length > 0) {
    return resumeGroups.value.map((group) => ({
      ...group,
      resumes: group.resumes.map((resume) => ({
        ...resume,
        time: resume.time || formatDate(stats.lastEditedDate),
      })),
    }))
  }

  const allResumes = []

  // 主简历放入第一组
  if (masterResume) {
    allResumes.push({
      id: 'master',
      title: masterResume.title || '主简历',
      snippet: masterResume.contentSnippet?.slice(0, 80) || '暂无内容',
      initial: 'M',
      time: formatDate(stats.lastEditedDate),
      groupId: 'group-master',
      groupName: '我的简历',
      score: 0,
    })
  }

  // 定制简历
  tailoredResumes.forEach((resume, index) => {
    allResumes.push({
      id: resume.id || `custom-${index}`,
      title: resume.title || '未命名简历',
      snippet: resume.jobSnippet?.slice(0, 80) || resume.contentSnippet?.slice(0, 80) || '暂无内容',
      initial: getMonogram(resume.title || '简历'),
      time: getRelativeTime(resume.updatedAt || stats.lastEditedDate),
      groupId: 'group-custom',
      groupName: '定制简历',
      score: resume.score || 0,
    })
  })

  // 按分组返回
  const result = []

  if (allResumes.filter((r) => r.groupId === 'group-master').length > 0) {
    result.push({
      groupId: 'group-master',
      groupName: '我的简历',
      count: allResumes.filter((r) => r.groupId === 'group-master').length,
      resumes: allResumes.filter((r) => r.groupId === 'group-master'),
    })
  }

  if (allResumes.filter((r) => r.groupId === 'group-custom').length > 0) {
    result.push({
      groupId: 'group-custom',
      groupName: '定制简历',
      count: allResumes.filter((r) => r.groupId === 'group-custom').length,
      resumes: allResumes.filter((r) => r.groupId === 'group-custom'),
    })
  }

  return result
})
</script>

<template>
  <div class="dashboard">
    <AppTabBar />
    <div class="container">
      <header class="page-header">
        <h1>我的简历工作台</h1>
      </header>

      <!-- AI 助手卡片 -->
      <div class="ai-assist-grid">
        <router-link to="/ai-tools/resume/wizard" class="ai-card">
          <div class="ai-card-inner">
            <div class="ai-icon-wrap green">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/><circle cx="12" cy="12" r="3"/></svg>
            </div>
            <div class="ai-content">
              <h3>AI 生成简历</h3>
              <p>通过 AI 对话智能生成简历内容，包含职业测评与岗位匹配</p>
            </div>
            <div class="ai-arrow">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </div>
          </div>
        </router-link>

        <router-link to="/ai-tools/resume/workspace" class="ai-card">
          <div class="ai-card-inner">
            <div class="ai-icon-wrap purple">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16v16H4z"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>
            </div>
            <div class="ai-content">
              <h3>简历制作</h3>
              <p>使用开源 AIResume 编辑器，分区填写、拖拽排序并实时预览简历</p>
            </div>
            <div class="ai-arrow">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </div>
          </div>
        </router-link>

        <router-link :to="{ path: '/ai-tools/resume/workspace', query: { tab: 'templates' } }" class="ai-card">
          <div class="ai-card-inner">
            <div class="ai-icon-wrap amber">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
            </div>
            <div class="ai-content">
              <h3>模板市场</h3>
              <p>浏览并使用开源项目保留的五套原始简历模板</p>
            </div>
            <div class="ai-arrow">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </div>
          </div>
        </router-link>
      </div>

      <!-- 简历分组管理 -->
      <div class="section group-section-wrapper">
        <div class="section-header">
          <h2 class="section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
            简历分组管理
          </h2>
          <button class="btn-new-group" type="button" @click="openGroupModal">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新建分组
          </button>
        </div>

        <!-- 分组列表 -->
        <div class="group-content">
          <div v-for="item in groupedResumes" :key="item.groupId" class="group-list">
            <h3 class="group-name">
              {{ item.groupName }}
              <span class="group-count">{{ item.count }} 份</span>
            </h3>
            <div class="project-grid">
              <div
                v-for="resume in item.resumes"
                :key="resume.id"
                class="project-card editable"
                @click="$router.push('/ai-tools/resume/designer')"
              >
                <div class="project-header">
                  <div
                    class="project-avatar"
                    :style="{
                      backgroundColor: cardColors[hashTitle(resume.title) % cardColors.length].bg,
                      color: cardColors[hashTitle(resume.title) % cardColors.length].fg,
                    }"
                  >
                    {{ resume.initial }}
                  </div>
                  <span class="project-badge badge-ready">已就绪</span>
                </div>
                <h3 class="project-title">{{ resume.title }}</h3>
                <p class="project-snippet">{{ resume.snippet }}</p>
                <div class="project-meta">
                  <span class="project-date">更新：{{ resume.time }}</span>
                  <div class="resume-score">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                    <span>{{ resume.score }}分</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 新建分组弹窗 -->
      <Teleport to="body">
        <Transition name="modal-fade">
          <div v-if="showGroupModal" class="modal-overlay" @click.self="closeGroupModal">
            <div class="modal-card">
              <div class="modal-header">
                <h3 class="modal-title">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                  新建分组
                </h3>
                <button class="modal-close" type="button" aria-label="关闭" @click="closeGroupModal">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>

              <div class="modal-body">
                <label class="input-label" for="group-name">分组名称</label>
                <div class="input-wrapper" :class="{ 'input-wrapper--error': groupNameError }">
                  <input
                    id="group-name"
                    v-model="newGroupName"
                    type="text"
                    maxlength="20"
                    placeholder="请输入分组名称，例如：前端开发工程师"
                    @input="handleGroupInput"
                    @keydown.enter="handleCreateGroup"
                  />
                </div>
                <p v-if="groupNameError" class="input-error">{{ groupNameError }}</p>
                <p v-else class="input-hint">分组名称不得超过20个字符</p>
              </div>

              <div class="modal-footer">
                <button class="btn-cancel" type="button" @click="closeGroupModal">取消</button>
                <button class="btn-confirm" type="button" @click="handleCreateGroup">确认</button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.container {
  display: flex;
  flex-direction: column;
  width: min(1200px, calc(100% - 48px));
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 88px 0 56px;
}

.page-header {
  margin-bottom: 26px;
}

.page-header h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

/* ===== AI 助手区域 ===== */
.ai-assist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.ai-card {
  display: flex;
  padding: 28px 32px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-cream);
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}

.ai-card:hover {
  transform: translateY(-2px);
  background: #f7f2e8;
}

.ai-card-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.ai-icon-wrap {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  border-radius: var(--hp-r-md);
  color: var(--hp-ink);
  flex-shrink: 0;
}

.ai-icon-wrap.blue {
  background: var(--hp-blue);
}

.ai-icon-wrap.green {
  background: var(--hp-green);
}

.ai-icon-wrap.purple {
  background: var(--hp-pink);
}

.ai-icon-wrap.amber {
  background: var(--hp-yellow);
}

.ai-content {
  flex: 1;
  min-width: 0;
}

.ai-content h3 {
  margin: 0 0 6px;
  color: var(--hp-ink);
  font-size: 18px;
  font-weight: 600;
}

.ai-content p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.7;
}

.ai-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px solid var(--hp-line);
  border-radius: 50%;
  color: var(--hp-ink);
  background: transparent;
  white-space: nowrap;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s;
}

.ai-card:hover .ai-arrow {
  color: var(--hp-cream);
  background: var(--hp-ink);
}

/* ===== 简历分组管理 ===== */
.group-section-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--hp-cream);
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  padding: 28px 30px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
}

.section-title svg {
  color: var(--hp-ink);
}

.btn-new-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 18px;
  border: 1px solid var(--hp-ink);
  border-radius: 999px;
  color: var(--hp-cream);
  background: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.btn-new-group:hover {
  color: var(--hp-ink);
  background: transparent;
}

.group-list {
  margin-bottom: 28px;
}

.group-list:last-child {
  margin-bottom: 0;
}

.group-name {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.group-count {
  color: var(--hp-muted);
  font-size: 12px;
  font-weight: 500;
}

.group-list + .group-list {
  padding-top: 24px;
  border-top: 1px dashed rgba(23, 23, 23, 0.18);
}

.group-content {
  flex: 1;
}

/* ===== 区域标题 ===== */
.section {
  margin-bottom: 24px;
}

/* ===== 简历卡片网格 ===== */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.project-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 22px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: var(--hp-r-md);
  background: #f7f2e8;
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease;
  overflow: hidden;
  min-height: 180px;
}

.project-card:hover {
  border-color: var(--hp-line);
  transform: translateY(-2px);
}

.project-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.project-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
}

.project-badge {
  padding: 4px 12px;
  border-radius: 999px;
  color: var(--hp-ink);
  font-size: 11px;
  font-weight: 600;
}

.badge-ready {
  background: var(--hp-green);
}

.badge-processing {
  background: var(--hp-blue);
}

.project-title {
  margin: 0 0 6px;
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-snippet {
  margin: 0 0 16px;
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.project-date {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.project-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid rgba(23, 23, 23, 0.1);
}

.resume-score {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 999px;
  color: var(--hp-ink);
  background: #f6edd2;
  font-size: 12px;
  font-weight: 600;
}

.resume-score svg {
  fill: var(--hp-yellow);
  stroke: var(--hp-ink);
}

.project-card.editable:hover {
  border-color: var(--hp-line);
}

/* ===== 新建分组弹窗 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  background: rgba(23, 23, 23, 0.42);
}

.modal-card {
  width: min(460px, calc(100% - 32px));
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-cream);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 26px;
  border-bottom: 1px solid rgba(23, 23, 23, 0.12);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
}

.modal-title svg {
  color: var(--hp-ink);
}

.modal-close {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: 50%;
  color: var(--hp-muted);
  background: transparent;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.modal-close:hover {
  color: var(--hp-ink);
  background: rgba(23, 23, 23, 0.06);
}

.modal-body {
  padding: 26px;
}

.input-label {
  display: block;
  margin-bottom: 10px;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.input-wrapper {
  display: flex;
  align-items: center;
  padding: 2px;
  border: 1px solid rgba(23, 23, 23, 0.24);
  border-radius: 999px;
  background: transparent;
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--hp-ink);
}

.input-wrapper--error {
  border-color: #b4544c;
}

.input-wrapper--error:focus-within {
  border-color: #b4544c;
}

.input-wrapper input {
  flex: 1;
  width: 100%;
  padding: 12px 14px;
  border: none;
  border-radius: 999px;
  color: var(--hp-ink);
  background: transparent;
  font-size: 14px;
  outline: none;
}

.input-wrapper input::placeholder {
  color: #a8a196;
}

.input-hint {
  margin: 10px 0 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.input-error {
  margin: 10px 0 0;
  color: #a54239;
  font-size: 12px;
  font-weight: 500;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 26px;
  border-top: 1px solid rgba(23, 23, 23, 0.12);
}

.btn-cancel {
  min-height: 38px;
  padding: 0 20px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink);
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.btn-cancel:hover {
  background: rgba(23, 23, 23, 0.06);
}

.btn-confirm {
  min-height: 38px;
  padding: 0 22px;
  border: 1px solid var(--hp-ink);
  border-radius: 999px;
  color: var(--hp-cream);
  background: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.btn-confirm:hover {
  color: var(--hp-ink);
  background: transparent;
}

/* 弹窗过渡动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* ===== 响应式 ===== */

@media (max-width: 560px) {
  .container {
    width: calc(100% - 32px);
    padding: 82px 0 40px;
  }

  .ai-card {
    padding: 22px;
  }

  .ai-card-inner {
    gap: 14px;
  }

  .ai-icon-wrap {
    width: 50px;
    height: 50px;
  }

  .ai-arrow {
    display: none;
  }

  .group-section-wrapper {
    padding: 22px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }
}
</style>
