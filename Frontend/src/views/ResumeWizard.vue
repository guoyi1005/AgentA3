<script setup>
import { ref, reactive, computed, nextTick, onMounted, watch } from 'vue'
import AppTabBar from '../components/AppTabBar.vue'

const messages = ref([
  {
    role: 'ai',
    text: '你好！我是 AI 简历向导。我会通过几个问题帮你创建一份专业的简历。首先，请告诉我你想要应聘的职位是什么？',
    time: '刚刚',
  },
])

// 向导按固定顺序收集 5 项简历信息，回答顺序与提问顺序一一对应
const questions = [
  { key: 'targetPosition' },
  { key: 'project' },
  { key: 'achievement' },
  { key: 'skills' },
  { key: 'education' },
]

const followUpReplies = [
  '感谢你的分享！接下来我们聊聊工作经历。请描述你最自豪的一个项目——你在其中扮演了什么角色，用了哪些技术，取得了什么成果？',
  '很好！现在来补充一些细节。在这个项目中，你有没有量化的工作成果？比如性能提升了多少，用户量增长了多少？',
  '接下来我们看看技能部分。除了你提到的技术栈，你还有哪些证书、语言能力或者其他特长？',
  '最后一步了！请告诉我你的教育经历——学校、专业、学位，以及在校期间有什么突出的成就或奖项？',
]

const finishMessage =
  '太好了，所有问题已经回答完毕！我已根据你的回答整理出简历草稿，可以在右侧「实时预览」中查看。'

const inputText = ref('')
const isThinking = ref(false)
const questionCount = ref(0)
const totalQuestions = questions.length
const progress = ref(0)
const finished = ref(false)
const chatContainer = ref(null)

const resume = reactive({
  targetPosition: '',
  project: '',
  achievement: '',
  skills: '',
  education: '',
})

const skillList = computed(() =>
  resume.skills
    .split(/[,，、;；/\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 12),
)

const hasResumeData = computed(() =>
  Object.values(resume).some((value) => String(value).trim() !== ''),
)

const previewSections = computed(() => {
  const list = []
  if (resume.project) list.push({ title: '项目经历', text: resume.project })
  if (resume.achievement) list.push({ title: '量化成果', text: resume.achievement })
  if (resume.education) list.push({ title: '教育经历', text: resume.education })
  return list
})

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isThinking.value || finished.value) return

  messages.value.push({ role: 'user', text, time: '刚刚' })
  inputText.value = ''
  isThinking.value = true

  // 把本次回答归档到对应字段，右侧预览实时更新
  const current = questions[questionCount.value]
  if (current) resume[current.key] = text

  questionCount.value++
  progress.value = Math.round((questionCount.value / totalQuestions) * 100)

  await new Promise((resolve) => setTimeout(resolve, 1200 + Math.random() * 800))
  isThinking.value = false

  if (questionCount.value < totalQuestions) {
    messages.value.push({
      role: 'ai',
      text: followUpReplies[questionCount.value - 1],
      time: '刚刚',
    })
  } else {
    finished.value = true
    messages.value.push({ role: 'ai', text: finishMessage, time: '刚刚' })
  }

  await nextTick()
  scrollToBottom()
}

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

onMounted(() => scrollToBottom())

watch(messages, () => nextTick(() => scrollToBottom()), { deep: true })
</script>

<template>
  <div class="wizard">
    <AppTabBar />
    <div class="wizard-layout">
      <!-- 左侧聊天 -->
      <div class="chat-panel">
        <header class="chat-header">
          <router-link to="/ai-tools/resume" class="back-link">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
            返回
          </router-link>
          <h1>AI 简历向导</h1>
          <div class="progress-badge">{{ progress }}%</div>
        </header>

        <div class="progress-bar-wrap">
          <div class="progress-bar-fill" :style="{ width: progress + '%' }"></div>
        </div>

        <div class="chat-area" ref="chatContainer">
          <div v-for="(msg, i) in messages" :key="i" :class="['chat-bubble', msg.role === 'user' ? 'chat-user' : 'chat-ai']">
            <template v-if="msg.role === 'ai'">
              <div class="chat-avatar">AI</div>
              <div class="chat-content">
                <p class="chat-text">{{ msg.text }}</p>
                <span class="chat-time">{{ msg.time }}</span>
              </div>
            </template>
            <template v-else>
              <div class="chat-content">
                <p class="chat-text">{{ msg.text }}</p>
                <span class="chat-time">{{ msg.time }}</span>
              </div>
              <div class="chat-avatar user-avatar">我</div>
            </template>
          </div>

          <div v-if="isThinking" class="chat-bubble chat-ai">
            <div class="chat-avatar">AI</div>
            <div class="chat-content">
              <div class="thinking-dots"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <div class="input-bar">
          <div class="input-row">
            <textarea
              v-model="inputText"
              :placeholder="finished ? '已完成全部问题，简历已在右侧生成' : '输入你的回答...'"
              :disabled="isThinking || finished"
              @keydown.enter.exact.prevent="sendMessage"
              rows="2"
            ></textarea>
            <button class="send-btn" :disabled="!inputText.trim() || isThinking || finished" @click="sendMessage">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg>
            </button>
          </div>
          <div class="input-actions">
            <span class="input-hint" v-if="finished">已完成全部问题</span>
            <span class="input-hint" v-else>{{ questionCount }}/{{ totalQuestions }} 个问题</span>
          </div>
        </div>
      </div>

      <!-- 右侧预览 -->
      <div class="preview-panel">
        <div class="preview-header">
          <h2>实时预览</h2>
        </div>
        <div class="preview-content">
          <div class="preview-empty" v-if="!hasResumeData">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <p>回答几个问题后，简历将在这里实时展示</p>
          </div>
          <div class="preview-card" v-else>
            <p class="preview-target" v-if="resume.targetPosition">求职意向：{{ resume.targetPosition }}</p>
            <section v-for="section in previewSections" :key="section.title" class="preview-section">
              <div class="preview-section-head">
                <span class="preview-bar"></span>{{ section.title }}
              </div>
              <p class="preview-text">{{ section.text }}</p>
            </section>
            <section v-if="skillList.length" class="preview-section">
              <div class="preview-section-head">
                <span class="preview-bar"></span>技能证书
              </div>
              <div class="preview-tags">
                <span v-for="tag in skillList" :key="tag" class="preview-tag">{{ tag }}</span>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wizard {
  min-height: 100vh;
  padding-top: 62px;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.wizard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  grid-template-rows: minmax(0, 1fr);
  height: calc(100vh - 62px);
  width: min(1440px, calc(100% - 48px));
  padding: 20px 0 24px;
  gap: 20px;
  margin: 0 auto;
}

/* ===== 左侧聊天 ===== */
.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-cream);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(23, 23, 23, 0.12);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--hp-muted);
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
}

.back-link:hover {
  color: var(--hp-ink);
}

.chat-header h1 {
  flex: 1;
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.progress-badge {
  padding: 4px 12px;
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-yellow);
  font-size: 12px;
  font-weight: 600;
}

.progress-bar-wrap {
  height: 3px;
  background: rgba(23, 23, 23, 0.08);
}

.progress-bar-fill {
  height: 100%;
  background: var(--hp-ink);
  transition: width 0.4s ease;
}

.chat-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-bubble {
  display: flex;
  gap: 10px;
  max-width: 75%;
}

.chat-ai { align-self: flex-start; }

.chat-user {
  align-self: flex-end;
}

.chat-avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--hp-line);
  border-radius: 10px;
  color: var(--hp-ink);
  background: var(--hp-blue);
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-avatar { background: var(--hp-green); }

.chat-text {
  margin: 0;
  padding: 12px 16px;
  border-radius: 14px;
  color: var(--hp-ink);
  background: #f3eee3;
  border: 1px solid rgba(23, 23, 23, 0.12);
  font-size: 14px;
  line-height: 1.7;
}

.chat-user .chat-text {
  color: var(--hp-cream);
  background: var(--hp-ink);
  border: 0;
}

.chat-time {
  color: var(--hp-muted);
  font-size: 11px;
  padding: 0 4px;
  margin-top: 4px;
  display: block;
}

.chat-user .chat-time { text-align: right; }

.thinking-dots {
  display: flex;
  gap: 5px;
  padding: 14px 18px;
}

.thinking-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a8a196;
  animation: dotBounce 1.4s ease-in-out infinite;
}

.thinking-dots span:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ===== 输入栏 ===== */
.input-bar {
  padding: 16px 24px;
  border-top: 1px solid rgba(23, 23, 23, 0.12);
}

.input-row {
  display: flex;
  gap: 10px;
}

textarea {
  flex: 1;
  min-height: 48px;
  max-height: 100px;
  padding: 12px 16px;
  border: 1px solid rgba(23, 23, 23, 0.24);
  border-radius: 14px;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  font-family: inherit;
  transition: border-color 0.18s ease;
}

textarea:focus {
  border-color: var(--hp-ink);
}

textarea::placeholder {
  color: #a8a196;
}

.send-btn {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 0;
  color: var(--hp-cream);
  background: var(--hp-ink);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}

.send-btn:hover { background: #2f2f2f; }
.send-btn:disabled { background: #d8d1c4; cursor: not-allowed; }

.input-actions {
  margin-top: 10px;
}

.input-hint {
  color: var(--hp-muted);
  font-size: 12px;
}

/* ===== 右侧预览 ===== */
.preview-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-cream);
  overflow: hidden;
}

.preview-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(23, 23, 23, 0.12);
}

.preview-header h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 16px;
  font-weight: 600;
}

.preview-content {
  flex: 1;
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
}

.preview-card {
  padding: 26px 22px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: #fffdf8;
  font-size: 13px;
  line-height: 1.7;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 80px 40px;
  text-align: center;
}

.preview-empty p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 14px;
  line-height: 1.6;
}

.preview-empty svg {
  stroke: #c9c3b8;
}

.preview-target {
  margin: 0 0 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(23, 23, 23, 0.12);
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 600;
}

.preview-section {
  margin-bottom: 18px;
}

.preview-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--hp-ink);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.preview-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--hp-ink);
}

.preview-text {
  margin: 0;
  padding-left: 11px;
  color: #55504a;
  font-size: 13px;
  line-height: 1.6;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-left: 11px;
}

.preview-tag {
  padding: 3px 10px;
  border-radius: 999px;
  color: #4c473f;
  background: rgba(23, 23, 23, 0.06);
  font-size: 12px;
  font-weight: 500;
}

/* ===== 响应式 ===== */

@media (max-width: 1080px) {
  .wizard-layout {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: 72vh auto;
    height: auto;
    gap: 18px;
    padding-bottom: 32px;
  }
}

@media (max-width: 640px) {
  .wizard-layout {
    width: calc(100% - 32px);
  }

  .chat-header,
  .preview-header {
    padding: 16px 18px;
  }

  .chat-area,
  .preview-content {
    padding: 18px;
  }

  .input-bar {
    padding: 14px 18px;
  }

  .chat-bubble {
    max-width: 88%;
  }
}
</style>
