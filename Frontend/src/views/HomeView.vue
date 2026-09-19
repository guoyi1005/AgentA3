<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'
import { getLatestJobRecommendations, JOB_BOSS_CTA, JOB_SALARY_HINT, resolveBossJobSearchLink, resolveBossJobSearchLinkFromJob } from '../api/jobRecommendations'
import { getUserInfo } from '../utils/auth'

const router = useRouter()
const searchKeyword = ref('')
const hotJobsLoading = ref(true)
const hotJobs = ref([])

const hotSearches = ['AI算法', 'Java开发', '前端架构', '云原生', '产品经理', '数据分析']

const categoryPages = [
  [
    {
      id: 'it_ai',
      main: '互联网与人工智能',
      sub: '前端 后端 AI算法 产品 运营',
    },
    {
      id: 'chip',
      main: '电子与通信技术',
      sub: '硬件 芯片 通信 网络',
    },
    {
      id: 'finance',
      main: '金融与保险',
      sub: '银行 投资 风控 审计',
    },
    {
      id: 'education',
      main: '教育与培训',
      sub: '教师 教研 留学 心理',
    },
    {
      id: 'health',
      main: '医疗与健康',
      sub: '临床 护理 医技 康复',
    },
    {
      id: 'biotech',
      main: '生物制药与化工',
      sub: '基因 制药 化学 质检',
    },
  ],
  [
    {
      id: 'manufacturing',
      main: '制造业与工业生产',
      sub: '机械 电气 生产 供应链',
    },
    {
      id: 'automobile',
      main: '汽车与交通装备',
      sub: '研发 测试 智驾 服务',
    },
    {
      id: 'construction',
      main: '建筑工程与地产',
      sub: '设计 施工 造价 物业',
    },
    {
      id: 'energy',
      main: '能源矿业与环保',
      sub: '电力 新能源 环保 安全',
    },
    {
      id: 'retail',
      main: '电商与零售',
      sub: '选品 运营 直播 门店',
    },
    {
      id: 'marketing',
      main: '市场广告与公关',
      sub: '品牌 投流 内容 活动',
    },
  ],
  [
    {
      id: 'media',
      main: '文化传媒与内容',
      sub: '编辑 摄像 编导 自媒体',
    },
    {
      id: 'design',
      main: '艺术与设计',
      sub: '平面 三维 室内 交互',
    },
    {
      id: 'legal',
      main: '法律咨询与知识产权',
      sub: '律师 法务 咨询 合规',
    },
    {
      id: 'admin',
      main: '企业管理与行政',
      sub: '行政 人事 秘书 经理',
    },
    {
      id: 'sales',
      main: '销售与客户服务',
      sub: '大客户 渠道 客服 商务',
    },
    {
      id: 'logistics',
      main: '物流仓储与供应链',
      sub: '仓储 配送 采购 报关',
    },
  ],
  [
    {
      id: 'hospitality',
      main: '餐饮酒店与旅游',
      sub: '厨师 酒店 导游 会展',
    },
    {
      id: 'public',
      main: '公共服务与政府',
      sub: '社区 外事 消防 应急',
    },
    {
      id: 'sports',
      main: '体育与健身',
      sub: '教练 康复 赛事 电竟',
    },
    {
      id: 'service',
      main: '家政与生活服务',
      sub: '月嫂 维修 美业 宠物',
    },
    {
      id: 'security',
      main: '安保与应急服务',
      sub: '安检 消防 安全 风险',
    },
    {
      id: 'freelance',
      main: '自由职业与新兴职业',
      sub: '自媒体 写手 AI创作 顾问',
    },
  ],
]

const categoryDetails = {
  it_ai: {
    title: '互联网与人工智能',
    groups: [
      { name: '开发与技术', tags: ['前端开发工程师', '后端开发工程师', '全栈工程师', '测试工程师', '运维工程师'] },
      { name: 'AI与算法', tags: ['算法工程师', '机器学习工程师', '大模型工程师', 'AI应用工程师', '提示词工程师'] },
      { name: '产品与设计', tags: ['产品经理', '数据分析师', 'UI设计师', 'UX设计师'] },
    ],
  },
  chip: {
    title: '电子与通信技术',
    groups: [
      { name: '研发与设计', tags: ['电子工程师', '通信工程师', '嵌入式工程师', 'PCB设计工程师'] },
      { name: '测试与运维', tags: ['射频工程师', '设备测试工程师', '网络优化工程师', '技术支持'] },
    ],
  },
  finance: {
    title: '金融与保险',
    groups: [
      { name: '金融业务', tags: ['投资经理', '风控专员', '财富顾问', '保险顾问'] },
      { name: '财务支持', tags: ['审计专员', '财务分析师', '税务专员', '会计'] },
    ],
  },
  education: {
    title: '教育与培训',
    groups: [
      { name: '教学岗位', tags: ['学科教师', '课程顾问', '教研老师', '升学顾问'] },
      { name: '支持岗位', tags: ['班主任', '教学运营', '心理咨询师'] },
    ],
  },
  health: {
    title: '医疗与健康',
    groups: [
      { name: '临床方向', tags: ['临床医生', '护士', '康复治疗师', '医技人员'] },
      { name: '健康服务', tags: ['健康管理师', '营养师', '心理咨询师'] },
    ],
  },
  biotech: {
    title: '生物制药与化工',
    groups: [
      { name: '研发岗位', tags: ['生物研发工程师', '制药工程师', '化学分析师'] },
      { name: '质量岗位', tags: ['质量专员', '检验工程师', '注册申报专员'] },
    ],
  },
  manufacturing: {
    title: '制造业与工业生产',
    groups: [
      { name: '生产方向', tags: ['机械工程师', '工艺工程师', '设备工程师', '生产主管'] },
      { name: '供应链方向', tags: ['计划专员', '采购专员', '质量工程师'] },
    ],
  },
  automobile: {
    title: '汽车与交通装备',
    groups: [
      { name: '研发方向', tags: ['整车工程师', '智驾工程师', '测试工程师'] },
      { name: '服务方向', tags: ['售后工程师', '服务顾问', '供应链专员'] },
    ],
  },
  construction: {
    title: '建筑工程与地产',
    groups: [
      { name: '工程方向', tags: ['建筑设计师', '施工员', '造价工程师', '项目经理'] },
      { name: '地产方向', tags: ['招商主管', '物业经理', '策划专员'] },
    ],
  },
  energy: {
    title: '能源矿业与环保',
    groups: [
      { name: '能源方向', tags: ['电气工程师', '新能源工程师', '储能工程师'] },
      { name: '环保方向', tags: ['环保工程师', 'EHS专员', '安全工程师'] },
    ],
  },
  retail: {
    title: '电商与零售',
    groups: [
      { name: '电商方向', tags: ['电商运营', '选品专员', '直播运营', '投流专员'] },
      { name: '零售方向', tags: ['门店店长', '陈列专员', '招商主管'] },
    ],
  },
  marketing: {
    title: '市场广告与公关',
    groups: [
      { name: '品牌方向', tags: ['品牌经理', '媒介经理', '活动策划', '广告优化师'] },
      { name: '内容方向', tags: ['内容运营', '文案策划', '公关专员'] },
    ],
  },
  media: {
    title: '文化传媒与内容',
    groups: [
      { name: '内容方向', tags: ['编辑', '编导', '摄像师', '新媒体运营'] },
      { name: '创作方向', tags: ['短视频策划', '主播', '后期剪辑'] },
    ],
  },
  design: {
    title: '艺术与设计',
    groups: [
      { name: '视觉方向', tags: ['平面设计师', '三维设计师', '插画师'] },
      { name: '空间方向', tags: ['室内设计师', '展陈设计师', '交互设计师'] },
    ],
  },
  legal: {
    title: '法律咨询与知识产权',
    groups: [
      { name: '法律方向', tags: ['律师', '法务', '合规专员', '知识产权顾问'] },
      { name: '咨询方向', tags: ['咨询顾问', '项目顾问'] },
    ],
  },
  admin: {
    title: '企业管理与行政',
    groups: [
      { name: '行政方向', tags: ['行政专员', '前台', '秘书', '总助'] },
      { name: '人力方向', tags: ['招聘专员', 'HRBP', '培训专员'] },
    ],
  },
  sales: {
    title: '销售与客户服务',
    groups: [
      { name: '销售方向', tags: ['大客户经理', '渠道经理', '招商主管', '商务经理'] },
      { name: '服务方向', tags: ['客服专员', '售后专员', '呼叫中心专员'] },
    ],
  },
  logistics: {
    title: '物流仓储与供应链',
    groups: [
      { name: '仓配方向', tags: ['仓储主管', '配送专员', '物流专员'] },
      { name: '供应链方向', tags: ['采购专员', '计划专员', '报关专员'] },
    ],
  },
  hospitality: {
    title: '餐饮酒店与旅游',
    groups: [
      { name: '餐饮方向', tags: ['厨师', '餐厅经理', '店长'] },
      { name: '文旅方向', tags: ['酒店管家', '导游', '会展执行'] },
    ],
  },
  public: {
    title: '公共服务与政府',
    groups: [
      { name: '服务方向', tags: ['社区工作者', '外事专员', '政务服务专员'] },
      { name: '应急方向', tags: ['消防员', '应急专员'] },
    ],
  },
  sports: {
    title: '体育与健身',
    groups: [
      { name: '训练方向', tags: ['健身教练', '体育教练', '康复师'] },
      { name: '赛事方向', tags: ['赛事运营', '裁判', '场馆管理员'] },
    ],
  },
  service: {
    title: '家政与生活服务',
    groups: [
      { name: '家庭服务', tags: ['家政服务员', '月嫂', '育婴师'] },
      { name: '生活服务', tags: ['维修师傅', '美甲师', '宠物美容师'] },
    ],
  },
  security: {
    title: '安保与应急服务',
    groups: [
      { name: '安保方向', tags: ['保安', '安检员', '安全管理员'] },
      { name: '风险方向', tags: ['风险评估师', '安全工程师'] },
    ],
  },
  freelance: {
    title: '自由职业与新兴职业',
    groups: [
      { name: '创作方向', tags: ['自由撰稿人', '独立设计师', '自媒体博主', 'AI内容创作者'] },
      { name: '服务方向', tags: ['线上顾问', '配音员', '翻译'] },
    ],
  },
}

const displayHotJobs = computed(() => hotJobs.value.slice(0, 3))

const hotDirections = computed(() => {
  const directions = []
  const seen = new Set()
  for (const job of hotJobs.value) {
    const query = String(job?.jobTitle || '').trim()
    if (!query || seen.has(query)) continue
    seen.add(query)
    directions.push({
      label: query,
      query,
      skills: parseJobSkills(job.skills),
    })
  }
  if (directions.length) {
    return directions.slice(0, 6)
  }
  return hotSearches.map((item) => ({
    label: item,
    query: item,
    skills: [item],
  }))
})

const hotJobsWeekLabel = computed(() => {
  const first = hotJobs.value[0]
  if (!first?.weekStartDate || !first?.weekEndDate) return ''
  return `${String(first.weekStartDate).slice(0, 10)} — ${String(first.weekEndDate).slice(0, 10)}`
})

function parseJobSkills(skillsText) {
  return String(skillsText || '')
    .split(/[,，、]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function resolveJobSearchLink(job) {
  return resolveBossJobSearchLinkFromJob(job)
}

function openBossSearch(keyword) {
  const query = String(keyword || '').trim() || '软件工程师'
  window.open(resolveBossJobSearchLink(query), '_blank', 'noopener,noreferrer')
}

async function loadHotJobs() {
  hotJobsLoading.value = true
  try {
    const result = await getLatestJobRecommendations()
    hotJobs.value = Array.isArray(result?.data) ? result.data : []
  } catch {
    hotJobs.value = []
  } finally {
    hotJobsLoading.value = false
  }
}

onMounted(loadHotJobs)

const currentPage = ref(0)
const activeCategoryId = ref('')
const detailPinned = ref(false)

const pageCount = computed(() => categoryPages.length)
const currentCategories = computed(() => categoryPages[currentPage.value] ?? [])
const activeCategory = computed(() => categoryDetails[activeCategoryId.value] ?? null)

function changePage(step) {
  const nextPage = currentPage.value + step
  if (nextPage < 0 || nextPage >= pageCount.value) {
    return
  }
  currentPage.value = nextPage
  activeCategoryId.value = ''
  detailPinned.value = false
}

function showCategory(id) {
  activeCategoryId.value = id
}

function resetPreview() {
  if (!detailPinned.value) {
    activeCategoryId.value = ''
  }
}

function keepPreview() {
  if (activeCategoryId.value) {
    detailPinned.value = true
  }
}

function releasePreview() {
  detailPinned.value = false
  activeCategoryId.value = ''
}

/* ============================================================
 * 首页展示层数据
 * 1) 问候语、日期、岗位分类编号来自现有数据与登录信息；
 * 2) 个人成长区（目标岗位 / 能力进度 / 今日计划 / 课程进度）
 *    后端暂无对应接口，这里统一集中为展示用示例数据，
 *    接入真实数据时只需替换下面这一段常量，其余结构与逻辑不变。
 * ============================================================ */

const userInfo = computed(() => getUserInfo() || {})
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '同学')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayLabel = computed(() => {
  const now = new Date()
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.getDay()]
  return `${now.getMonth() + 1} 月 ${now.getDate()} 日 · ${weekday}`
})

const focusJob = {
  title: 'Python 开发工程师',
  matchRate: 72,
  note: '距离目标岗位还差两项能力，先补齐 FastAPI 与项目实战。',
  skills: ['Python', 'MySQL', 'FastAPI', '项目实战'],
}

const todayPlan = ref([
  { id: 'plan-python-basic', title: 'Python 基础', meta: '已完成 2 个课时', state: 'done' },
  { id: 'plan-fastapi', title: 'FastAPI 实训', meta: '进行中 · 接口调试', state: 'doing' },
  { id: 'plan-interview', title: 'AI 模拟面试', meta: '安排在今天 20:00', state: 'todo' },
])

const isAddingPlan = ref(false)
const newPlanTitle = ref('')
const planInputRef = ref(null)

const myCourses = [
  { id: 'fastapi', title: 'FastAPI 接口开发', type: '实训课 · 12 课时', progress: 62, tone: 'pink', cover: 'api' },
  { id: 'mysql', title: 'MySQL 多表查询', type: '基础课 · 8 课时', progress: 45, tone: 'blue', cover: 'db' },
  { id: 'project', title: 'Python 项目实战', type: '项目课 · 6 个项目', progress: 28, tone: 'green', cover: 'code' },
]

const todayPlanDone = computed(() => todayPlan.value.filter((task) => task.state === 'done').length)

function togglePlanTask(task) {
  task.state = task.state === 'done' ? 'todo' : 'done'
}

async function startAddPlan() {
  isAddingPlan.value = true
  await nextTick()
  planInputRef.value?.focus()
}

function submitPlanTask() {
  const title = newPlanTitle.value.trim()
  if (title) {
    todayPlan.value.push({ id: `plan-${Date.now()}`, title, meta: '刚刚添加', state: 'todo' })
  }
  newPlanTitle.value = ''
  isAddingPlan.value = false
}

function cancelPlanTask() {
  newPlanTitle.value = ''
  isAddingPlan.value = false
}

function formatCategorySub(sub) {
  return String(sub || '')
    .split(/[\s、,，·]+/)
    .filter(Boolean)
    .join(' · ')
}

function categoryIndex(index) {
  return String(currentPage.value * 6 + index + 1).padStart(2, '0')
}

// 岗位探索：默认展示当前这一组的第一个方向，鼠标经过时切换，避免出现空白区
const featuredCategoryId = computed(() => activeCategoryId.value || currentCategories.value[0]?.id || '')
const featuredCategory = computed(() => categoryDetails[featuredCategoryId.value] ?? null)

// 推荐岗位：优先展示本周真实岗位，岗位雷达暂无数据时退回热门方向入口
const recommendJobs = computed(() => {
  if (displayHotJobs.value.length) {
    return displayHotJobs.value.map((job) => ({
      id: job.id || job.jobTitle,
      title: job.jobTitle,
      skills: parseJobSkills(job.skills),
      meta: JOB_SALARY_HINT,
      href: resolveJobSearchLink(job),
      cta: JOB_BOSS_CTA,
    }))
  }
  return hotDirections.value.slice(0, 3).map((direction) => ({
    id: direction.query,
    title: direction.label,
    skills: direction.skills,
    meta: '岗位方向 · 前往 BOSS 直聘查看真实公司与薪资',
    href: resolveBossJobSearchLink(direction.query),
    cta: '查看岗位',
  }))
})
</script>

<template>
  <div class="home-view">
    <AppTabBar embedded />

    <div class="hp-main">
      <!-- Hero 欢迎区 -->
      <section class="hp-hero">
        <div class="hp-hero__body">
          <p class="hp-eyebrow">{{ todayLabel }} · {{ greeting }}，{{ displayName }}</p>
          <h1 class="hp-hero__title">找准方向，再开始成长</h1>
          <p class="hp-hero__desc">
            上传简历，AI 分析你的能力差距，并生成岗位匹配结果与专属学习路径。
          </p>
          <div class="hp-hero__actions">
            <button class="hp-btn hp-btn--solid" type="button" @click="router.push('/interview/resume')">
              开始岗位体检
            </button>
            <button class="hp-link" type="button" @click="router.push('/career/nebula')">
              查看岗位星图 →
            </button>
          </div>

          <form class="hp-search" @submit.prevent="openBossSearch(searchKeyword)">
            <svg class="hp-search__icon" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="6.5" />
              <path d="M16 16.2 20.4 20.6" />
            </svg>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索职位、公司，例如：AI 大模型工程师"
            />
            <button type="submit">搜索</button>
          </form>

          <p class="hp-hotline">
            <span class="hp-hotline__label">热门</span>
            <template v-for="(item, index) in hotSearches" :key="item">
              <span v-if="index" class="hp-hotline__sep">·</span>
              <button class="hp-hotline__item" type="button" @click="openBossSearch(item)">{{ item }}</button>
            </template>
          </p>
        </div>

        <div class="hp-hero__visual">
          <svg class="hp-hero__art" viewBox="0 0 320 300" role="img" aria-label="学习中的学生插画">
                <circle cx="196" cy="142" r="112" fill="#EAD574" />
                <path d="M126 244c0-44 14-70 34-70s34 26 34 70z" fill="#BED2E4" stroke="#171717" stroke-width="3" />
                <circle cx="160" cy="140" r="30" fill="#FBF8F2" stroke="#171717" stroke-width="3" />
              <path d="M130 136c2-20 14-30 30-30s28 10 30 30c-8-8-18-11-30-11s-22 3-30 11z" fill="#171717" />
              <circle cx="150" cy="141" r="2.6" fill="#171717" />
              <circle cx="170" cy="141" r="2.6" fill="#171717" />
              <path
                d="M152 152c4 4 12 4 16 0"
                fill="none"
                stroke="#171717"
                stroke-width="2.6"
                stroke-linecap="round"
              />
              <path
                d="M112 244l16-48h64l16 48z"
                fill="#FBF8F2"
                stroke="#171717"
                stroke-width="3"
                stroke-linejoin="round"
              />
              <path d="M122 238l12-34h52l12 34z" fill="#BCC99C" />
              <path d="M134 206c-10 8-16 22-18 34" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round" />
              <path d="M186 206c10 8 16 22 18 34" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round" />
              <path
                d="M262 244l4-18h20l4 18z"
                fill="#EEC3CF"
                stroke="#171717"
                stroke-width="3"
                stroke-linejoin="round"
              />
              <path
                d="M276 224c0-16-8-24-18-24 0 12 8 22 18 24z"
                fill="#BCC99C"
                stroke="#171717"
                stroke-width="3"
                stroke-linejoin="round"
              />
          </svg>
        </div>
      </section>

      <!-- 岗位探索 -->
      <section class="hp-section">
        <header class="hp-section__head">
          <h2 class="hp-section__title">岗位探索</h2>
          <div class="hp-pager">
            <button type="button" :disabled="currentPage === 0" @click="changePage(-1)">上一组</button>
            <span class="hp-pager__num">{{ currentPage + 1 }} / {{ pageCount }}</span>
            <button type="button" :disabled="currentPage === pageCount - 1" @click="changePage(1)">下一组</button>
          </div>
        </header>

        <ul class="hp-cats" @mouseleave="resetPreview">
          <li v-for="(item, index) in currentCategories" :key="item.id">
            <button
              class="hp-cat"
              :class="{ 'is-active': featuredCategoryId === item.id }"
              type="button"
              @mouseenter="showCategory(item.id)"
              @focus="showCategory(item.id)"
            >
              <span class="hp-cat__num">{{ categoryIndex(index) }}</span>
              <span class="hp-cat__main">{{ item.main }}</span>
              <span class="hp-cat__sub">{{ formatCategorySub(item.sub) }}</span>
            </button>
          </li>
        </ul>

        <div
          v-if="featuredCategory"
          class="hp-cat-detail"
          @mouseenter="keepPreview"
          @mouseleave="releasePreview"
        >
          <p class="hp-cat-detail__title">{{ featuredCategory.title }}</p>
          <div v-for="group in featuredCategory.groups" :key="group.name" class="hp-cat-detail__group">
            <p class="hp-cat-detail__group-name">{{ group.name }}</p>
            <div class="hp-tags">
              <span v-for="tag in group.tags" :key="tag" class="hp-tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 继续成长 -->
      <section class="hp-section">
        <header class="hp-section__head">
          <h2 class="hp-section__title">继续成长</h2>
        </header>

        <div class="hp-growth">
          <article class="hp-card hp-target">
            <header class="hp-card__head">
              <h3 class="hp-card__title">我的目标岗位</h3>
              <span class="hp-badge">{{ focusJob.matchRate }}% 匹配</span>
            </header>
            <p class="hp-target__job">{{ focusJob.title }}</p>
            <p class="hp-target__note">{{ focusJob.note }}</p>
            <div class="hp-tags">
              <span v-for="skill in focusJob.skills" :key="skill" class="hp-tag">{{ skill }}</span>
            </div>
            <button class="hp-link hp-link--start" type="button" @click="router.push('/career/nebula')">
              查看岗位星图 →
            </button>
          </article>

          <article class="hp-card hp-plan">
            <header class="hp-card__head">
              <h3 class="hp-card__title">今日计划</h3>
              <div class="hp-plan__tools">
                <span class="hp-plan__count">{{ todayPlanDone }} / {{ todayPlan.length }}</span>
                <button
                  class="hp-plan__add"
                  type="button"
                  aria-label="添加今日计划"
                  title="添加今日计划"
                  @click="startAddPlan"
                >+</button>
              </div>
            </header>
            <ul class="hp-plan__list">
              <li v-for="task in todayPlan" :key="task.id">
                <button
                  class="hp-plan__item"
                  :class="`is-${task.state}`"
                  type="button"
                  :aria-pressed="task.state === 'done'"
                  :title="task.state === 'done' ? '点击标记为未完成' : '点击标记为已完成'"
                  @click="togglePlanTask(task)"
                >
                  <span class="hp-plan__mark"></span>
                  <span class="hp-plan__title">{{ task.title }}</span>
                  <span class="hp-plan__meta">{{ task.meta }}</span>
                </button>
              </li>
              <li v-if="isAddingPlan" class="hp-plan__edit">
                <span class="hp-plan__mark hp-plan__mark--todo"></span>
                <input
                  ref="planInputRef"
                  v-model="newPlanTitle"
                  type="text"
                  maxlength="40"
                  placeholder="输入计划内容，回车保存"
                  @keyup.enter="submitPlanTask"
                  @keyup.esc="cancelPlanTask"
                />
              </li>
            </ul>
          </article>
        </div>
      </section>

      <!-- 继续学习 -->
      <section class="hp-section">
        <header class="hp-section__head">
          <h2 class="hp-section__title">继续学习</h2>
          <button class="hp-link" type="button" @click="router.push('/career/nebula/python')">
            全部课程 →
          </button>
        </header>

        <div class="hp-courses__grid">
            <button
              v-for="course in myCourses"
              :key="course.id"
              class="hp-course"
              type="button"
              @click="router.push('/career/nebula/python')"
            >
              <span class="hp-course__cover" :class="`is-${course.tone}`">
                <svg v-if="course.cover === 'api'" viewBox="0 0 320 200" aria-hidden="true">
                  <rect x="66" y="46" width="188" height="108" rx="14" fill="#FBF8F2" stroke="#171717" stroke-width="3" />
                  <path d="M66 76h188" stroke="#171717" stroke-width="3" />
                  <circle cx="84" cy="61" r="3.5" fill="#171717" />
                  <circle cx="96" cy="61" r="3.5" fill="#171717" />
                  <circle cx="108" cy="61" r="3.5" fill="#171717" />
                  <rect x="84" y="92" width="76" height="7" rx="3.5" fill="#BED2E4" />
                  <rect x="84" y="110" width="112" height="7" rx="3.5" fill="#BCC99C" />
                  <rect x="84" y="128" width="56" height="7" rx="3.5" fill="#EAD574" />
                </svg>
                <svg v-else-if="course.cover === 'db'" viewBox="0 0 320 200" aria-hidden="true">
                  <path
                    d="M102 62v76c0 11 26 20 58 20s58-9 58-20V62"
                    fill="#FBF8F2"
                    stroke="#171717"
                    stroke-width="3"
                  />
                  <ellipse cx="160" cy="62" rx="58" ry="20" fill="#FBF8F2" stroke="#171717" stroke-width="3" />
                  <path d="M102 90c0 11 26 20 58 20s58-9 58-20" fill="none" stroke="#171717" stroke-width="3" />
                  <path d="M102 118c0 11 26 20 58 20s58-9 58-20" fill="none" stroke="#171717" stroke-width="3" />
                </svg>
                <svg v-else viewBox="0 0 320 200" aria-hidden="true">
                  <path
                    d="M120 68 94 100l26 32"
                    fill="none"
                    stroke="#171717"
                    stroke-width="6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                  <path
                    d="M200 68l26 32-26 32"
                    fill="none"
                    stroke="#171717"
                    stroke-width="6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                  <path d="M176 58l-32 84" fill="none" stroke="#171717" stroke-width="6" stroke-linecap="round" />
                </svg>
              </span>
              <span class="hp-course__title">{{ course.title }}</span>
              <span class="hp-course__type">{{ course.type }}</span>
              <span class="hp-course__progress">
                <span class="hp-bar__track"><i :style="{ width: `${course.progress}%` }"></i></span>
                <span class="hp-course__value">{{ course.progress }}%</span>
              </span>
            </button>
        </div>
      </section>

      <!-- 推荐岗位 -->
      <section class="hp-section">
        <header class="hp-section__head">
          <h2 class="hp-section__title">推荐岗位</h2>
          <div class="hp-section__aside">
            <span v-if="hotJobsWeekLabel" class="hp-section__meta">{{ hotJobsWeekLabel }}</span>
            <button class="hp-link" type="button" @click="router.push('/jobs/hot')">查看全部 →</button>
          </div>
        </header>

        <p v-if="hotJobsLoading" class="hp-jobs__state">正在整理本周岗位…</p>

        <ul v-else class="hp-jobs__grid">
          <li v-for="(job, index) in recommendJobs" :key="job.id" class="hp-job">
            <span class="hp-job__logo" :class="`is-tone-${index % 4}`">{{ job.title.charAt(0) }}</span>
            <div class="hp-job__copy">
              <p class="hp-job__title">{{ job.title }}</p>
              <p class="hp-job__meta">{{ job.meta }}</p>
              <div class="hp-tags">
                <span v-for="item in job.skills" :key="item" class="hp-tag">{{ item }}</span>
              </div>
            </div>
            <a class="hp-link hp-job__link" :href="job.href" target="_blank" rel="noreferrer">
              {{ job.cta }} →
            </a>
          </li>
        </ul>
      </section>

      <footer class="hp-footer">
        <p>© 2026 数智诊断港 | 本平台数据仅用于学术研究与个人职业发展规划</p>
        <p>ICP备案号：粤 ICP 备 XXXXXXX 号</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.home-view {
  position: relative;
  min-height: 100vh;
  background: var(--hp-bg);
  color: var(--hp-ink);
}

.home-view *,
.home-view *::before,
.home-view *::after {
  box-sizing: border-box;
}

.hp-main {
  width: min(1440px, calc(100% - 48px));
  margin: 0 auto;
  padding: 30px 0 64px;
}

/* ---------- Hero 欢迎区 ---------- */

.hp-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 42%);
  align-items: stretch;
  gap: 24px;
  padding: 42px 36px 36px 44px;
  border: 1px solid var(--hp-line);
  border-radius: 26px;
  background: var(--hp-pink-soft);
}

.hp-hero__body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.hp-eyebrow {
  margin: 0 0 16px;
  color: #8a7f7f;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.hp-hero__title {
  margin: 0;
  font-size: 46px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.hp-hero__desc {
  margin: 20px 0 0;
  max-width: 26em;
  color: #55504a;
  font-size: 16px;
  line-height: 1.8;
}

.hp-hero__actions {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-top: 30px;
}

.hp-search {
  display: flex;
  align-items: center;
  gap: 12px;
  width: min(100%, 540px);
  margin-top: 30px;
  padding: 5px 5px 5px 18px;
  border: 1px solid var(--hp-line);
  border-radius: 20px;
  background: var(--hp-cream);
}

.hp-search__icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  fill: none;
  stroke: var(--hp-ink);
  stroke-width: 1.6;
  stroke-linecap: round;
}

.hp-search input {
  flex: 1;
  min-width: 0;
  height: 42px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
}

.hp-search input::placeholder {
  color: #a8a196;
}

.hp-search button {
  flex: 0 0 auto;
  height: 42px;
  padding: 0 26px;
  border-radius: 999px;
  background: var(--hp-ink);
  color: var(--hp-cream);
  font-size: 14px;
  font-weight: 600;
}

/* ---------- 热门搜索轻量行 ---------- */

.hp-hotline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: 14px 0 0;
  font-size: 13px;
}

.hp-hotline__label {
  margin-right: 4px;
  color: #9a9388;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.hp-hotline__item {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-muted);
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
}

.hp-hotline__item:hover {
  color: var(--hp-ink);
  text-decoration: underline;
}

.hp-hotline__sep {
  color: #c9c3b8;
}

/* ---------- 区块标题 ---------- */

.hp-section {
  margin-top: 40px;
}

.hp-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.hp-section__title {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.hp-pager {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-pager button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-muted);
  font-size: 12px;
  cursor: pointer;
}

.hp-pager button:hover:not(:disabled) {
  color: var(--hp-ink);
}

.hp-pager button:disabled {
  color: #cfc9bd;
  cursor: not-allowed;
}

.hp-pager__num {
  color: #a8a196;
}

.hp-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

/* ---------- 岗位探索（横向内容卡） ---------- */

.hp-cats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-cats li {
  display: flex;
}

.hp-cat {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  padding: 20px;
  border: 1px solid transparent;
  border-radius: var(--hp-r-md);
  background: var(--hp-cream);
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.hp-cat:hover {
  transform: translateY(-2px);
}

.hp-cat.is-active {
  border-color: var(--hp-line);
  background: var(--hp-yellow);
}

.hp-cat__num {
  color: #a8a196;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.hp-cat__main {
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
}

.hp-cat__sub {
  margin-top: auto;
  padding-top: 8px;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.65;
}

.hp-cat.is-active .hp-cat__num,
.hp-cat.is-active .hp-cat__sub {
  color: #6a5f3a;
}

.hp-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 24px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.hp-btn:hover {
  background: var(--hp-ink);
  color: var(--hp-cream);
}

.hp-btn--solid {
  background: var(--hp-ink);
  color: var(--hp-cream);
}

.hp-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
}

.hp-link:hover {
  text-decoration: underline;
}

.hp-link--start {
  align-self: flex-start;
  margin-top: auto;
}

.hp-hero__visual {
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.hp-hero__art {
  display: block;
  width: min(100%, 400px);
  height: auto;
}

/* 岗位方向详情：跟随上方卡片切换，横向铺开 */

.hp-cat-detail {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 16px 40px;
  min-height: 218px;
  margin-top: 16px;
  padding: 24px 26px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-cream);
}

.hp-cat-detail__title {
  flex: 0 0 100%;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.hp-cat-detail__group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 200px;
}

.hp-cat-detail__group-name {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
  letter-spacing: 0.06em;
}

.hp-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hp-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(23, 23, 23, 0.06);
  color: #4c473f;
  font-size: 12px;
  line-height: 1.6;
  white-space: nowrap;
}

/* ---------- 继续成长 ---------- */

.hp-growth {
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(0, 5fr);
  gap: var(--hp-gap);
  align-items: stretch;
}

.hp-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 26px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-cream);
}

.hp-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.hp-section__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-section__aside {
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.hp-badge {
  flex: 0 0 auto;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 12px;
  font-weight: 600;
}

.hp-target__job {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.hp-target__note {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.7;
}

.hp-bar__track {
  display: block;
  height: 6px;
  border-radius: 999px;
  background: rgba(23, 23, 23, 0.09);
  overflow: hidden;
}

.hp-bar__track > i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--hp-ink);
}

/* ---------- 今日计划（轻量任务摘要） ---------- */

.hp-plan__tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.hp-plan__count {
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-plan__add {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(23, 23, 23, 0.24);
  border-radius: 50%;
  background: transparent;
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.hp-plan__add:hover {
  border-color: var(--hp-ink);
  background: var(--hp-ink);
  color: var(--hp-cream);
}

.hp-plan__list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-plan__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 0;
  border: 0;
  border-bottom: 1px solid rgba(23, 23, 23, 0.08);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.hp-plan__list > li:last-child .hp-plan__item {
  padding-bottom: 0;
  border-bottom: 0;
}

.hp-plan__mark {
  position: relative;
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  border-radius: 50%;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.hp-plan__item.is-done .hp-plan__mark {
  background: var(--hp-green);
}

.hp-plan__item.is-done .hp-plan__mark::after {
  content: '';
  width: 5px;
  height: 9px;
  border-right: 1.6px solid var(--hp-ink);
  border-bottom: 1.6px solid var(--hp-ink);
  transform: translateY(-1px) rotate(45deg);
}

.hp-plan__item.is-doing .hp-plan__mark {
  border: 4px solid var(--hp-yellow);
}

.hp-plan__item.is-todo .hp-plan__mark,
.hp-plan__mark--todo {
  border: 1.5px solid rgba(23, 23, 23, 0.28);
}

.hp-plan__title {
  font-size: 14px;
  font-weight: 600;
  transition: color 0.18s ease;
}

.hp-plan__meta {
  margin-left: auto;
  color: var(--hp-muted);
  font-size: 12px;
  white-space: nowrap;
}

.hp-plan__item.is-done .hp-plan__title {
  color: #9c968b;
}

.hp-plan__item:hover .hp-plan__title {
  color: #8a6d1f;
}

.hp-plan__item:hover .hp-plan__mark {
  border-color: var(--hp-ink);
}

.hp-plan__edit {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0 0;
}

.hp-plan__edit input {
  flex: 1;
  min-width: 0;
  height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(23, 23, 23, 0.24);
  border-radius: 999px;
  background: transparent;
  color: var(--hp-ink);
  font-size: 13px;
  outline: none;
  transition: border-color 0.18s ease;
}

.hp-plan__edit input::placeholder {
  color: #a8a196;
}

.hp-plan__edit input:focus {
  border-color: var(--hp-ink);
}

.hp-courses__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.hp-course {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease;
}

.hp-course:hover {
  transform: translateY(-2px);
}

.hp-course__cover {
  display: block;
  aspect-ratio: 16 / 10;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  overflow: hidden;
}

.hp-course__cover.is-pink {
  background: var(--hp-pink);
}

.hp-course__cover.is-blue {
  background: var(--hp-blue);
}

.hp-course__cover.is-green {
  background: var(--hp-green);
}

.hp-course__cover svg {
  display: block;
  width: 100%;
  height: 100%;
}

.hp-course__title {
  font-size: 17px;
  font-weight: 600;
}

.hp-course__type {
  color: var(--hp-muted);
  font-size: 13px;
}

.hp-course__progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}

.hp-course__progress .hp-bar__track {
  flex: 1;
}

.hp-course__progress .hp-bar__track > i {
  background: var(--hp-green);
}

.hp-course__value {
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-jobs__state {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.hp-jobs__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-job {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
  border-radius: var(--hp-r-md);
  background: #f7f2e8;
}

.hp-job__logo {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
}

.hp-job__logo.is-tone-0 {
  background: var(--hp-pink);
}

.hp-job__logo.is-tone-1 {
  background: var(--hp-blue);
}

.hp-job__logo.is-tone-2 {
  background: var(--hp-green);
}

.hp-job__logo.is-tone-3 {
  background: var(--hp-yellow);
}

.hp-job__copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.hp-job__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.hp-job__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.6;
}

.hp-job__link {
  align-self: flex-start;
  margin-top: auto;
}

/* ---------- 页脚 ---------- */

.hp-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 36px;
  padding-top: 20px;
  border-top: 1px solid rgba(23, 23, 23, 0.14);
  color: #9a9388;
  font-size: 12px;
}

.hp-footer p {
  margin: 0;
}

/* ---------- 响应式 ---------- */

@media (max-width: 1200px) {
  .hp-growth {
    grid-template-columns: minmax(0, 1fr);
  }

  .hp-jobs__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1000px) {
  .hp-hero {
    grid-template-columns: minmax(0, 1fr);
    padding: 32px 26px 26px;
  }

  .hp-hero__visual {
    align-items: center;
    justify-content: flex-start;
  }

  .hp-cats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hp-courses__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .hp-main {
    width: calc(100% - 32px);
    padding: 22px 0 48px;
  }

  .hp-hero__title {
    font-size: 32px;
  }

  .hp-hero__desc {
    font-size: 15px;
  }

  .hp-cats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hp-courses__grid,
  .hp-jobs__grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .hp-plan__item {
    flex-wrap: wrap;
  }

  .hp-plan__meta {
    margin-left: 32px;
    white-space: normal;
  }

  .hp-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
