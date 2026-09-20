<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'

const router = useRouter()
const route = useRoute()

// 是否处于“仅模板市场”模式（?tab=templates），呈现迁移前的整页模板市场
const isTemplatesMode = computed(() => route.query.tab === 'templates')

// 接收嵌入应用发来的“已选择模板”通知，模板市场模式下自动跳回工作台
const handleFrameMessage = (event) => {
  const data = event.data
  if (!data || data.type !== 'aiResumeTemplateSelected') return
  if (!isTemplatesMode.value) return
  if (!data.templateId) return
  console.log('✅ 已选择模板，跳转到工作台:', data.templateId)
  router.push('/ai-tools/resume/workspace')
}

// 模板列表数据 - 从 templates.json 加载
const templates = ref([])

// 当前选中的模板 ID
const selectedTemplateId = ref('202501')

// 简历表单 iframe 引用
const formFrameRef = ref(null)

// 待切换模板 ID（桥接尚未就绪时暂存，iframe 加载后自动应用）
const pendingTemplateId = ref(null)

// 处理点击模板
const handleTemplateClick = (templateId) => {
  selectedTemplateId.value = templateId
  pendingTemplateId.value = templateId
  switchTemplateInFrame(templateId)
}

// 通过 iframe 内的桥接，直接调用模板市场页面原有的切换模板函数
const switchTemplateInFrame = (templateId, retries = 5) => {
  const frame = formFrameRef.value
  const win = frame && frame.contentWindow
  const bridge = win && win.__resumeBridge
  if (bridge && bridge.switchTemplate(templateId)) {
    pendingTemplateId.value = null
    console.log('✅ 已切换到模板:', templateId)
    return true
  }
  if (retries > 0) {
    setTimeout(() => switchTemplateInFrame(templateId, retries - 1), 200)
  } else {
    console.warn('❌ 模板切换失败，桥接不可用:', templateId)
  }
  return false
}

// 向 iframe 注入桥接脚本，暴露模板市场原有的切换模板能力
const injectResumeBridge = () => {
  const frame = formFrameRef.value
  const doc = frame && frame.contentDocument
  if (!doc || doc.getElementById('__resume_bridge_script__')) return
  const script = doc.createElement('script')
  script.id = '__resume_bridge_script__'
  script.textContent = `
    ;(function () {
      if (window.__resumeBridge) return
      var getResumeStore = function () {
        var appEl = document.getElementById('app')
        var app = appEl && appEl.__vue_app__
        if (!app || !app._context || !app._context.provides) return null
        var provides = app._context.provides
        var keys = Reflect.ownKeys(provides)
        for (var i = 0; i < keys.length; i++) {
          var value = provides[keys[i]]
          if (value && value._s && typeof value._s.get === 'function') {
            var store = value._s.get('resume')
            if (store && typeof store.updateResumeSetting === 'function') return store
          }
        }
        return null
      }
      window.__resumeBridge = {
        switchTemplate: function (id) {
          var store = getResumeStore()
          if (!store) return false
          store.updateResumeSetting({ currentTemplate: id })
          return true
        },
        getCurrentTemplate: function () {
          var store = getResumeStore()
          return store ? store.resumeSetting.currentTemplate : null
        }
      }
      var installTemplateNotifier = function (attempt) {
        var store = getResumeStore()
        if (store && typeof store.$onAction === 'function') {
          store.$onAction(function (ctx) {
            var patch = ctx.args[0]
            if (ctx.name === 'updateResumeSetting' && patch && patch.currentTemplate) {
              ctx.after(function () {
                try {
                  window.parent.postMessage({ type: 'aiResumeTemplateSelected', templateId: patch.currentTemplate }, '*')
                } catch (e) {}
              })
            }
          })
        } else if (attempt < 10) {
          setTimeout(function () { installTemplateNotifier(attempt + 1) }, 200)
        }
      }
      installTemplateNotifier(0)
    })()
  `
  doc.head.appendChild(script)
}

/* ===== 主题：内嵌页面跟随本站奶油色系，夜间模式跟随内嵌应用右下角的切换按钮 ===== */
// 深色状态由内嵌应用自己维护（它会 class="dark" 并写入自身 localStorage），这里只做镜像
const isDark = ref(false)
let themeObserver = null
let appliedTheme = ''
let earlyThemeTimer = null

// 内嵌应用自带的变量名，这里按本站配色重写
const FRAME_THEME_VARS = {
  light: `
    --bg-color: #f5f0e7 !important;
    --bg-card-color: #fbf8f2 !important;
    --text-color: #171717 !important;
    --text-color2: #fbf8f2 !important;
    --primary-color: #171717 !important;
    --primary-color-hover: #2f2f2f !important;
    --primary-color-active: #000000 !important;
    --card-color: #fbf8f2 !important;
    --color-1: #3c4750 !important;
    --color-2: #55636e !important;
    --color-3: #77868f !important;
    --color-4: #9dafbb !important;
    --color-5: #bccbd6 !important;
    --color-6: #d7e2e9 !important;
    --color-7: #eef3f6 !important;
    --chat-bg: #f5f0e7 !important;
    --chat-user-bubble: #171717 !important;
    --chat-ai-bubble: #fbf8f2 !important;
    --chat-bubble-shadow: rgba(23, 23, 23, 0.06) !important;
    --chat-input-bg: #fbf8f2 !important;
    --chat-border: rgba(23, 23, 23, 0.16) !important;
    --chat-input-text: #171717 !important;
    --chat-placeholder: #a8a196 !important;
  `,
  dark: `
    --bg-color: #171717 !important;
    --bg-card-color: #242424 !important;
    --text-color: #f5f0e7 !important;
    --text-color2: #171717 !important;
    --primary-color: #ead574 !important;
    --primary-color-hover: #e2cc66 !important;
    --primary-color-active: #d6bf55 !important;
    --card-color: #242424 !important;
    --color-1: #e6dfd2 !important;
    --color-2: #cfc7b8 !important;
    --color-3: #b3ab9d !important;
    --color-4: #8d8579 !important;
    --color-5: #5c5c5c !important;
    --color-6: #3d3d3d !important;
    --color-7: #2a2a2a !important;
    --chat-bg: #171717 !important;
    --chat-user-bubble: #ead574 !important;
    --chat-ai-bubble: #242424 !important;
    --chat-bubble-shadow: rgba(0, 0, 0, 0.3) !important;
    --chat-input-bg: #242424 !important;
    --chat-border: rgba(245, 240, 231, 0.18) !important;
    --chat-input-text: #f5f0e7 !important;
    --chat-placeholder: #8d8579 !important;
  `,
}

const FRAME_BASE_CSS = `
  /* 隐藏内嵌应用的顶部导航栏（简历制作/模板市场/AI深度交流/网站配置/简历模板设计） */
  header.navbar { display: none !important; }

  /* 撑满高度，底部不留空白 */
  .resume[data-v-d9239fc1] { height: 100vh !important; }

  /* 顶部按钮排右移，为左上角的返回按钮留出空间 */
  .btn-group[data-v-d9239fc1] {
    gap: 12px !important;
    padding-left: 130px !important;
    justify-content: center !important;
  }
  .btn-group[data-v-d9239fc1] .ant-btn {
    padding-left: 10px !important;
    padding-right: 10px !important;
  }
`

const FRAME_COMPONENT_CSS = {
  light: `
    body { background-color: var(--bg-color) !important; }
    .ant-btn { border-radius: 999px !important; }
    .ant-btn-primary,
    .ant-btn-primary:not(:disabled):focus,
    .ant-btn-primary:not(:disabled):active {
      background-color: #171717 !important;
      border-color: #171717 !important;
      color: #fbf8f2 !important;
    }
    .ant-btn-primary:not(:disabled):hover {
      background-color: #2f2f2f !important;
      border-color: #2f2f2f !important;
    }
    .ant-btn-primary.ant-btn-background-ghost {
      background: transparent !important;
      color: #171717 !important;
      border-color: #171717 !important;
    }
    .ant-btn-primary.ant-btn-background-ghost:not(:disabled):hover {
      color: #2f2f2f !important;
      border-color: #2f2f2f !important;
    }
    .ant-btn-dangerous {
      color: #a54239 !important;
      border-color: #dcb6b1 !important;
      background: transparent !important;
    }
    .ant-btn-link, a { color: #171717 !important; }
    .ant-input, .ant-select-selector, .ant-picker, .ant-input-number {
      background-color: #fbf8f2 !important;
      color: #171717 !important;
    }
    .ant-input:focus, .ant-input-focused,
    .ant-select-focused .ant-select-selector,
    .ant-select:not(.ant-select-disabled):hover .ant-select-selector,
    .ant-picker:hover, .ant-picker-focused,
    .ant-input-number:hover, .ant-input-number-focused {
      border-color: #171717 !important;
      box-shadow: 0 0 0 2px rgba(23, 23, 23, 0.08) !important;
    }
    .ant-checkbox-checked .ant-checkbox-inner,
    .ant-radio-checked .ant-radio-inner,
    .ant-switch-checked {
      border-color: #171717 !important;
      background-color: #171717 !important;
    }
    .ant-radio-checked .ant-radio-inner::after { background-color: #171717 !important; }
    .ant-select-item-option-selected {
      background-color: rgba(234, 213, 116, 0.4) !important;
      color: #171717 !important;
    }
    .ant-collapse > .ant-collapse-item > .ant-collapse-header .ant-collapse-arrow,
    .ant-message .anticon { color: #171717 !important; }
    .ant-typography { color: #171717 !important; }

    /* 编辑器面板 / 表单控件统一为奶油色系（antd 默认灰蓝描边与文字） */
    body, .ant-upload-wrapper, .ant-upload-text, .ant-form-item-label > label { color: #171717 !important; }
    .ant-btn:not(.ant-btn-primary) { color: #171717 !important; }
    .ant-alert, .ant-alert-content, .ant-collapse, .ant-collapse-item, .ant-collapse-content,
    .ant-select, .ant-tour, .ant-upload, .ant-picker, .ant-input-number {
      color: #171717 !important;
    }
    .ant-input { border-color: rgba(23, 23, 23, 0.16) !important; }
    .ant-input-wrapper, .ant-input-group { color: #171717 !important; }
    .ant-select-selector, .ant-upload.ant-upload-select {
      border-color: rgba(23, 23, 23, 0.16) !important;
    }
    .upload-area { color: #6f6a60 !important; }
    .preview { background-color: #f7f2e8 !important; }
    .ant-upload.ant-upload-select { background-color: transparent !important; }
    .ant-btn-default, .ant-btn-dashed {
      background-color: #fbf8f2 !important;
      border-color: rgba(23, 23, 23, 0.16) !important;
    }
    .ant-btn-default:not(:disabled):hover {
      background-color: #f3eee3 !important;
      border-color: #171717 !important;
    }
    .ant-collapse {
      background-color: #fbf8f2 !important;
      border-color: rgba(23, 23, 23, 0.16) !important;
    }
    .ant-collapse > .ant-collapse-item { border-color: rgba(23, 23, 23, 0.12) !important; }
    .ant-collapse > .ant-collapse-item > .ant-collapse-header {
      background-color: #f7f2e8 !important;
      color: #171717 !important;
    }
    .ant-collapse-content {
      background-color: #fbf8f2 !important;
      border-top-color: rgba(23, 23, 23, 0.12) !important;
    }
    .ant-collapse-content-box { color: #171717 !important; }
    .ant-input-group-addon {
      background-color: #f3eee3 !important;
      border-color: rgba(23, 23, 23, 0.16) !important;
      color: #6f6a60 !important;
    }
    .ant-alert-info {
      background-color: #f7f2e8 !important;
      border-color: rgba(23, 23, 23, 0.16) !important;
    }
    .ant-alert-info .ant-alert-message,
    .ant-alert-info .ant-alert-description,
    .ant-alert-info .anticon { color: #171717 !important; }
    .ant-select-arrow, .module-drag-handle, .anticon-menu, .anticon-down,
    .anticon-plus, .anticon-close, .anticon-close-circle { color: #a8a196 !important; }
    .upload-area, .ant-upload.ant-upload-drag {
      background-color: #f7f2e8 !important;
      border-color: rgba(23, 23, 23, 0.2) !important;
    }
    .upload-area .anticon, .ant-upload-text, .ant-upload-hint { color: #6f6a60 !important; }
    .setting { background-color: rgba(23, 23, 23, 0.04) !important; }
    .ant-tour-inner {
      border: 1px solid #222222 !important;
      background-color: #fbf8f2 !important;
      box-shadow: none !important;
    }
    .ant-tour-title, .ant-tour-description, .ant-tour-content { color: #171717 !important; }
    .ant-tour-indicator.ant-tour-indicator-active { background-color: #171717 !important; }
    .ant-tour-indicator { background-color: rgba(23, 23, 23, 0.15) !important; }
    .ant-tooltip-inner, .ant-popover-inner { background-color: #fbf8f2 !important; color: #171717 !important; }
  `,
  dark: `
    body { background-color: var(--bg-color) !important; color: #f5f0e7 !important; }
    .ant-btn { border-radius: 999px !important; }
    .ant-btn-primary,
    .ant-btn-primary:not(:disabled):focus,
    .ant-btn-primary:not(:disabled):active {
      background-color: #ead574 !important;
      border-color: #ead574 !important;
      color: #171717 !important;
    }
    .ant-btn-primary:not(:disabled):hover {
      background-color: #e2cc66 !important;
      border-color: #e2cc66 !important;
    }
    .ant-btn-primary.ant-btn-background-ghost {
      background: transparent !important;
      color: #ead574 !important;
      border-color: #ead574 !important;
    }
    .ant-btn-primary.ant-btn-background-ghost:not(:disabled):hover {
      color: #e2cc66 !important;
      border-color: #e2cc66 !important;
    }
    .ant-btn-default, .ant-btn-dashed {
      background-color: #242424 !important;
      border-color: rgba(245, 240, 231, 0.22) !important;
      color: #f5f0e7 !important;
    }
    .ant-btn-dangerous {
      color: #e8a9a2 !important;
      border-color: rgba(232, 169, 162, 0.45) !important;
      background: transparent !important;
    }
    .ant-btn-link, a { color: #ead574 !important; }
    .ant-input, .ant-select-selector, .ant-picker, .ant-input-number {
      background-color: #242424 !important;
      color: #f5f0e7 !important;
      border-color: rgba(245, 240, 231, 0.2) !important;
    }
    .ant-input:focus, .ant-input-focused,
    .ant-select-focused .ant-select-selector,
    .ant-select:not(.ant-select-disabled):hover .ant-select-selector,
    .ant-picker:hover, .ant-picker-focused,
    .ant-input-number:hover, .ant-input-number-focused {
      border-color: #ead574 !important;
      box-shadow: 0 0 0 2px rgba(234, 213, 116, 0.16) !important;
    }
    .ant-collapse, .ant-collapse-item, .ant-collapse-content {
      background-color: transparent !important;
      border-color: rgba(245, 240, 231, 0.14) !important;
    }
    .ant-collapse-content-box, .ant-collapse-header { color: #f5f0e7 !important; }
    .ant-checkbox-inner, .ant-radio-inner {
      background-color: #242424 !important;
      border-color: rgba(245, 240, 231, 0.28) !important;
    }
    .ant-checkbox-checked .ant-checkbox-inner,
    .ant-radio-checked .ant-radio-inner,
    .ant-switch-checked {
      border-color: #ead574 !important;
      background-color: #ead574 !important;
    }
    .ant-select-item-option-selected {
      background-color: rgba(234, 213, 116, 0.18) !important;
      color: #ead574 !important;
    }
    .ant-card, .ant-modal-content, .ant-drawer-content, .ant-dropdown-menu,
    .ant-select-dropdown, .ant-tour-inner, .ant-popover-inner {
      background-color: #242424 !important;
      color: #f5f0e7 !important;
    }
    .ant-tour-content, .ant-tour-title, .ant-typography { color: #f5f0e7 !important; }
    .ant-message .anticon, .ant-collapse > .ant-collapse-item > .ant-collapse-header .ant-collapse-arrow {
      color: #ead574 !important;
    }

    /* 深色下的编辑器面板 / 表单控件 */
    body, .ant-upload-wrapper, .ant-upload-text, .ant-form-item-label > label { color: #f5f0e7 !important; }
    .ant-btn:not(.ant-btn-primary) { color: #f5f0e7 !important; }
    .ant-alert, .ant-alert-content, .ant-collapse, .ant-collapse-item, .ant-collapse-content,
    .ant-select, .ant-tour, .ant-upload, .ant-picker, .ant-input-number {
      color: #f5f0e7 !important;
    }
    .ant-input { border-color: rgba(245, 240, 231, 0.2) !important; }
    .ant-input-wrapper, .ant-input-group { color: #f5f0e7 !important; }
    .ant-select-selector, .ant-upload.ant-upload-select {
      border-color: rgba(245, 240, 231, 0.2) !important;
    }
    .upload-area { color: #b9b1a4 !important; }
    .preview { background-color: #1f1f1f !important; }
    .ant-upload.ant-upload-select { background-color: transparent !important; }
    .ant-collapse > .ant-collapse-item > .ant-collapse-header {
      background-color: #242424 !important;
      color: #f5f0e7 !important;
    }
    .ant-collapse-content-box { color: #f5f0e7 !important; }
    .ant-input-group-addon {
      background-color: #2a2a2a !important;
      border-color: rgba(245, 240, 231, 0.16) !important;
      color: #b9b1a4 !important;
    }
    .ant-alert-info {
      background-color: #242424 !important;
      border-color: rgba(245, 240, 231, 0.16) !important;
    }
    .ant-alert-info .ant-alert-message,
    .ant-alert-info .ant-alert-description,
    .ant-alert-info .anticon { color: #f5f0e7 !important; }
    .ant-select-arrow, .module-drag-handle, .anticon-menu, .anticon-down,
    .anticon-plus, .anticon-close, .anticon-close-circle { color: #8d8579 !important; }
    .upload-area, .ant-upload.ant-upload-drag {
      background-color: #242424 !important;
      border-color: rgba(245, 240, 231, 0.2) !important;
    }
    .upload-area .anticon, .ant-upload-text, .ant-upload-hint { color: #b9b1a4 !important; }
    .setting { background-color: rgba(245, 240, 231, 0.06) !important; }
    .ant-tour-inner {
      border: 1px solid rgba(245, 240, 231, 0.18) !important;
      box-shadow: none !important;
    }
    .ant-tour-indicator.ant-tour-indicator-active { background-color: #ead574 !important; }
    .ant-tour-indicator { background-color: rgba(245, 240, 231, 0.2) !important; }
  `,
}

const buildFrameStyle = (dark) => {
  const mode = dark ? 'dark' : 'light'
  return `${FRAME_BASE_CSS}
    :root { ${FRAME_THEME_VARS[mode]} }
    ${FRAME_COMPONENT_CSS[mode]}`
}

// 读取内嵌应用当前的深浅色状态
const readFrameTheme = () => {
  const frame = formFrameRef.value
  const doc = frame && frame.contentDocument
  if (!doc) return false
  const root = doc.documentElement
  return root.classList.contains('dark') || root.getAttribute('theme') === 'dark'
}

// 注入（或按当前深浅色重新注入）内嵌页面样式；内嵌应用切到深色时也要把本站覆盖样式换成深色版
const applyFrameStyleOverrides = (force = false) => {
  const frame = formFrameRef.value
  const doc = frame && frame.contentDocument
  if (!doc) return
  const dark = isDark.value
  const mode = dark ? 'dark' : 'light'
  if (!force && appliedTheme === mode && doc.getElementById('__resume_style_overrides__')) return
  appliedTheme = mode
  const existed = doc.getElementById('__resume_style_overrides__')
  if (existed) existed.remove()
  const style = doc.createElement('style')
  style.id = '__resume_style_overrides__'
  style.textContent = buildFrameStyle(dark)
  doc.head.appendChild(style)
}

// 同步内嵌应用的深浅色状态到本站（页面外壳跟着一起切换）
const syncFrameTheme = () => {
  isDark.value = readFrameTheme()
  applyFrameStyleOverrides(true)
}

// 监听内嵌应用切主题（右下角圆形按钮点击后会给 <html> 加 class="dark"）
const observeFrameTheme = () => {
  const frame = formFrameRef.value
  const doc = frame && frame.contentDocument
  if (!doc) return
  themeObserver?.disconnect()
  themeObserver = new MutationObserver(() => {
    if (readFrameTheme() !== isDark.value) syncFrameTheme()
  })
  themeObserver.observe(doc.documentElement, { attributes: true, attributeFilter: ['class', 'theme'] })
}

// iframe 完全加载（含字体/图片）较慢，这里在解析早期先尝试注入，避免先闪一下应用自带主题
const scheduleEarlyTheme = () => {
  window.clearTimeout(earlyThemeTimer)
  let attempts = 0
  const tick = () => {
    attempts += 1
    const doc = formFrameRef.value && formFrameRef.value.contentDocument
    if (doc && doc.head) {
      isDark.value = readFrameTheme()
      applyFrameStyleOverrides()
    }
    if (attempts < 12) earlyThemeTimer = window.setTimeout(tick, 350)
  }
  earlyThemeTimer = window.setTimeout(tick, 350)
}

// iframe 加载完成后注入桥接，并应用等待中的模板切换
const onFrameLoaded = () => {
  injectResumeBridge()
  isDark.value = readFrameTheme()
  appliedTheme = ''
  applyFrameStyleOverrides(true)
  observeFrameTheme()
  window.clearTimeout(earlyThemeTimer)
  // 同步嵌入应用当前模板到右侧选中态
  const frame = formFrameRef.value
  const bridge = frame && frame.contentWindow && frame.contentWindow.__resumeBridge
  if (bridge && typeof bridge.getCurrentTemplate === 'function' && !pendingTemplateId.value) {
    const current = bridge.getCurrentTemplate()
    if (current) selectedTemplateId.value = current
  }
  if (pendingTemplateId.value) {
    switchTemplateInFrame(pendingTemplateId.value)
  }
}

// 模板效果缩略图映射（与模板市场页面使用的资源一致）
const THUMBNAIL_MAP = {
  templateA: '/airesume/assets/preview-2zk25jB6.jpg',
  templateB: '/airesume/assets/preview-Cd6e9xvr.jpg',
  templateC: '/airesume/assets/preview-Ck76ZAWi.jpg',
  templateD: '/airesume/assets/preview-B2AHLd9n.jpg',
  dev: '/airesume/assets/preview-BhqGPQHX.jpg',
}

// 根据 folderPath 获取模板颜色
const getTemplateColor = (folderPath) => {
  const colorMap = {
    'templateA': '#165DFF',
    'templateB': '#0f4dbf',
    'templateC': '#4A90E8',
    'templateD': '#1e40af',
    'dev': '#3b82f6',
  }
  return colorMap[folderPath] || '#165DFF'
}

// 获取缩略图 URL
const getThumbnailUrl = (folderPath, fileName) => {
  return THUMBNAIL_MAP[folderPath] || `/airesume/${folderPath}/${fileName}`
}

// 图片加载失败时的回退处理
const handleImageError = (event, folderPath) => {
  // 如果缩略图加载失败，回退到默认模板缩略图
  event.target.src = THUMBNAIL_MAP[folderPath] || THUMBNAIL_MAP.templateA
  console.warn(`❌ 模板 ${folderPath} 的缩略图加载失败`)
}

// 加载模板列表
onMounted(async () => {
  window.addEventListener('message', handleFrameMessage)
  scheduleEarlyTheme()
  try {
    const response = await fetch('/airesume/templates.json')
    const data = await response.json()
    templates.value = data
    console.log('✅ 模板列表加载成功:', templates.value.length, '个模板')
  } catch (error) {
    console.error('❌ 模板列表加载失败:', error)
    // 如果加载失败，使用默认空数组
    templates.value = []
  }
})

onUnmounted(() => {
  window.removeEventListener('message', handleFrameMessage)
  themeObserver?.disconnect()
  window.clearTimeout(earlyThemeTimer)
})
</script>

<template>
  <div class="two-column-layout" :class="{ 'is-dark': isDark }">
    <AppTabBar />

    <!-- 左上角返回按钮（模板市场与工作台两种模式都显示） -->
    <div class="workspace-toolbar">
      <button class="workspace-back" type="button" aria-label="返回简历页面" @click="router.push('/ai-tools/resume')">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="m15 5-7 7 7 7" />
        </svg>
        <span>返回</span>
      </button>
    </div>

    <!-- 仅模板市场模式：整页展示模板市场（迁移前形态，不跳转） -->
    <template v-if="isTemplatesMode">
      <iframe
        ref="formFrameRef"
        src="/airesume/index.html?embedded=1#/template"
        title="简历模板市场"
        class="templates-full-frame"
        @load="onFrameLoaded"
      ></iframe>
    </template>

    <!-- 工作台模式：两栏布局（左侧简历填写与预览 + 右侧模板市场） -->
    <template v-else>
      <!-- 两栏布局容器：左侧表单 + 右侧模板市场 -->
      <div class="layout-container">
        <!-- 第 1 栏：简历填写与预览 -->
        <div class="left-panel">
          <iframe
            ref="formFrameRef"
            src="/airesume/index.html?embedded=1#/"
            title="简历信息录入"
            class="form-frame"
            @load="onFrameLoaded"
          ></iframe>
        </div>
        
        <!-- 第 2 栏：模板市场板块（新增）-->
        <div class="templates-panel">
          <div class="templates-header">
            <h3 class="templates-title">模板市场</h3>
            <p class="templates-subtitle">选择喜欢的简历模板</p>
          </div>
          
          <div class="templates-body">
            <div class="templates-grid">
              <div
                v-for="template in templates"
                :key="template.id"
                class="template-card"
                :class="{ active: selectedTemplateId === template.id }"
                @click="handleTemplateClick(template.id)"
              >
                <div class="template-preview-box">
                  <!-- 显示真实的简历缩略图 -->
                  <img 
                    :src="getThumbnailUrl(template.folderPath, template.thumbnail)"
                    :alt="template.name"
                    class="template-thumbnail"
                    @error="handleImageError($event, template.folderPath)"
                  />
                </div>
                <div class="template-info">
                  <div class="template-name">{{ template.name }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.two-column-layout {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.workspace-toolbar {
  position: fixed;
  top: 72px;
  left: 16px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 8px;
}

.workspace-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-cream);
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.workspace-back:hover {
  background: var(--hp-ink);
  color: var(--hp-cream);
}

.workspace-back svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.layout-container {
  display: flex;
  align-items: stretch;
  position: absolute;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  gap: 0; /* 无间隔，通过 padding 控制 */
}

/* ===== 第 1 栏：简历填写表单（原样保留）===== */
.left-panel {
  flex: 1;
  min-width: 450px;
  height: 100%;
  background-color: transparent;
}

.form-frame {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

/* 仅模板市场模式：整页模板市场 iframe */
.templates-full-frame {
  position: absolute;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: calc(100vh - 60px);
  border: none;
  display: block;
  background: var(--hp-bg);
}

/* ===== 第 2 栏：模板市场（关闭自动拉伸）===== */
.templates-panel {
  width: 220px;
  min-width: 220px;
  max-width: 250px;
  height: 100%;
  background-color: var(--hp-bg);
  border-left: 1px solid var(--hp-line);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-grow: 0; /* 关闭自动拉伸 */
}

.templates-header {
  padding: 12px 8px;
  border-bottom: 1px solid rgba(23, 23, 23, 0.12);
  background-color: var(--hp-cream);
  flex-shrink: 0;
}

.templates-title {
  margin: 0 0 4px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--hp-ink);
  text-align: center;
}

.templates-subtitle {
  margin: 0;
  font-size: 11px;
  color: var(--hp-muted);
  text-align: center;
}

.templates-body {
  flex: 1;
  overflow-y: auto;
  padding: 0; /* 消除左右空白 */
  display: flex;
  flex-direction: column;
}

/* 垂直均匀排列 */
.templates-grid {
  display: flex;
  flex-direction: column;
  gap: 12px; /* 均匀上下间距 */
  align-items: stretch;
  padding: 8px; /* 增加内边距适应缩略图 */
}

.template-card {
  cursor: pointer;
  transition: all 0.2s ease;
  width: 100%; /* 填满整个容器，左右不留白 */
}

.template-card:hover .template-preview-box {
  transform: translateY(-2px);
}

.template-card.active .template-preview-box {
  transform: scale(1.03); /* 稍微缩小缩放效果 */
}

/* 调整卡片高度 - 增大缩略图尺寸 */
.template-preview-box {
  border-radius: var(--hp-r-md);
  overflow: hidden;
  background-color: #fffdf8;
  border: 1px solid rgba(23, 23, 23, 0.16);
  transition: all 0.2s ease;
  aspect-ratio: 210 / 297; /* A4 纸张比例 */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
}

.template-card:hover .template-preview-box {
  border-color: var(--hp-ink);
}

.template-card.active .template-preview-box {
  border-color: var(--hp-ink);
}

.template-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.template-name {
  margin-top: 4px; /* 减小上边距 */
  text-align: center;
  font-size: 10px; /* 适应更窄容器 */
  font-weight: 500;
  color: var(--hp-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 模板信息区域 */
.template-info {
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* ===== 夜间模式（本站外壳）===== */

.two-column-layout.is-dark {
  background-color: #171717;
}

.two-column-layout.is-dark .templates-full-frame {
  background: #171717;
}

.two-column-layout.is-dark .workspace-back {
  background: #242424;
  border-color: rgba(245, 240, 231, 0.24);
  color: #f5f0e7;
}

.two-column-layout.is-dark .workspace-back:hover {
  background: #f5f0e7;
  border-color: #f5f0e7;
  color: #171717;
}

.two-column-layout.is-dark .templates-panel {
  background-color: #171717;
  border-left-color: rgba(245, 240, 231, 0.16);
}

.two-column-layout.is-dark .templates-header {
  background-color: #1f1f1f;
  border-bottom-color: rgba(245, 240, 231, 0.16);
}

.two-column-layout.is-dark .templates-title {
  color: #f5f0e7;
}

.two-column-layout.is-dark .templates-subtitle,
.two-column-layout.is-dark .template-name {
  color: #b9b1a4;
}

.two-column-layout.is-dark .template-preview-box {
  background-color: #242424;
  border-color: rgba(245, 240, 231, 0.16);
}

.two-column-layout.is-dark .template-card:hover .template-preview-box,
.two-column-layout.is-dark .template-card.active .template-preview-box {
  border-color: var(--hp-yellow);
}

</style>
