<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  getCareerExploration,
  getCareerNebulaMap,
} from '../api/careerNebula'
import AppTabBar from '../components/AppTabBar.vue'
import CareerPlanetView from './CareerPlanetView.vue'

const route = useRoute()
const router = useRouter()

const careers = ref([])
const skills = ref([])
const edges = ref([])
const loading = ref(true)
const loadError = ref('')
const selectedCareerId = ref('')
const selectedSkill = ref(null)
const showFullCareerDescription = ref(false)
const searchQuery = ref('')
const detailDescription = ref(null)
const descriptionOverflow = ref(false)
let latestLoadRequest = 0
let lastAutomaticRefreshAt = 0

const isLearningGalaxy = computed(() => Boolean(route.params.careerId))
const enabledCareers = computed(() => careers.value.filter((career) => career.status === 'enabled'))
const enabledSkills = computed(() => skills.value.filter((skill) => skill.status === 'enabled'))
const configuredSkills = computed(() => enabledSkills.value.filter((skill) => skill.configured))
const learningSummary = computed(() => {
  const totalChapters = enabledSkills.value.reduce((sum, skill) => sum + Number(skill.chapterCount || 0), 0)
  const completedChapters = enabledSkills.value.reduce(
    (sum, skill) => sum + Number(skill.completedChapterCount || 0),
    0,
  )
  const averageProgress = enabledCareers.value.length
    ? Math.round(enabledCareers.value.reduce((sum, career) => sum + careerProgress(career).percentage, 0) / enabledCareers.value.length)
    : 0
  return { totalChapters, completedChapters, averageProgress }
})
const chapterPercent = computed(() => {
  const total = learningSummary.value.totalChapters
  return total ? Math.round((learningSummary.value.completedChapters / total) * 100) : 0
})
const inProgressSkills = computed(() => configuredSkills.value
  .filter((skill) => Number(skill.explorationProgress || 0) > 0 && Number(skill.explorationProgress || 0) < 100)
  .sort((a, b) => Number(b.explorationProgress || 0) - Number(a.explorationProgress || 0)))
// 学习任务卡：优先展示进行中的星球，其次是尚未开始的星球
const taskSkills = computed(() => {
  const notStarted = configuredSkills.value
    .filter((skill) => Number(skill.explorationProgress || 0) <= 0)
    .sort((a, b) => Number(a.chapterCount || 0) - Number(b.chapterCount || 0))
  return [...inProgressSkills.value, ...notStarted].slice(0, 3)
})
const careerProgressRows = computed(() => enabledCareers.value
  .map((career) => ({ id: career.id, name: career.name, percentage: careerProgress(career).percentage }))
  .sort((a, b) => b.percentage - a.percentage))
const visibleCareers = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) return enabledCareers.value
  return enabledCareers.value.filter((career) => {
    return `${career.name || ''} ${career.description || ''}`.toLowerCase().includes(query)
  })
})
const activeCareer = computed(() => careers.value.find((career) => career.id === route.params.careerId))
const activeCareerSkills = computed(() => {
  if (!activeCareer.value) return []
  return skills.value.filter(
    (skill) => (skill.careerId || 'testing') === activeCareer.value.id && skill.status === 'enabled',
  )
})
const activeSkillIds = computed(() => new Set(activeCareerSkills.value.map((skill) => skill.id)))
const activeEdges = computed(() => edges.value.filter(
  (edge) => activeSkillIds.value.has(edge.source) && activeSkillIds.value.has(edge.target),
))
const selectedCareer = computed(() => careers.value.find((career) => career.id === selectedCareerId.value))
const enabledSkillsForCareer = (careerId) => skills.value.filter(
  (skill) => (skill.careerId || 'testing') === careerId && skill.status === 'enabled',
)

const skillProgress = (skill) => {
  return { total: skill?.chapterCount || 0, completed: skill?.completedChapterCount || 0,
    percentage: skill?.explorationProgress || 0 }
}

const careerProgress = (career) => {
  const planets = enabledSkillsForCareer(career?.id).filter((skill) => skill.configured)
  const percentage = planets.length
    ? Math.round(planets.reduce((sum, skill) => sum + (skill.explorationProgress || 0), 0) / planets.length) : 0
  return { percentage }
}

const nodeStyle = (node, compact = false) => ({
  left: `${node.x}%`,
  top: `${node.y}%`,
  width: `${Math.max(66, Number(node.size) || 88) * (compact ? 0.82 : 1)}px`,
  height: `${Math.max(66, Number(node.size) || 88) * (compact ? 0.82 : 1)}px`,
})

const edgeLine = (edge) => {
  const source = activeCareerSkills.value.find((skill) => skill.id === edge.source)
  const target = activeCareerSkills.value.find((skill) => skill.id === edge.target)
  if (!source || !target) return null
  return {
    x1: `${source.x}%`,
    y1: `${source.y}%`,
    x2: `${target.x}%`,
    y2: `${target.y}%`,
  }
}

const monogram = (name = '') => name.replace(/工程师|开发|测试|应用/g, '').slice(0, 2) || '星'

function selectCareer(career) {
  selectedCareerId.value = selectedCareerId.value === career.id ? '' : career.id
  showFullCareerDescription.value = false
}

function closeCareerDetail() {
  selectedCareerId.value = ''
  showFullCareerDescription.value = false
}

function enterLearningGalaxy(career) {
  router.push({ name: 'career-nebula', params: { careerId: career.id } })
}

const careerNameOf = (skill) => {
  const careerId = skill?.careerId || 'testing'
  return careers.value.find((career) => career.id === careerId)?.name || '岗位学习'
}

// 任务卡跳转：同一岗位星系直接打开星球，否则进入对应星系
async function openTaskSkill(skill) {
  const careerId = skill?.careerId || 'testing'
  if (route.params.careerId === careerId) {
    await openSkill(skill)
    return
  }
  router.push({ name: 'career-nebula', params: { careerId } })
}

function returnToCareerMap() {
  selectedSkill.value = null
  selectedCareerId.value = ''
  router.push({ name: 'career-nebula' })
}

async function openSkill(skill) {
  if (skill.status !== 'enabled') return
  await loadMap({ silent: true })
  const currentSkill = skills.value.find((item) => item.id === skill.id) || skill
  if (currentSkill.configured && activeCareer.value) {
    selectedSkill.value = currentSkill
    return
  }
  selectedSkill.value = currentSkill
}

function closeSkill() {
  selectedSkill.value = null
}

function checkDescriptionOverflow() {
  const element = detailDescription.value
  descriptionOverflow.value = Boolean(element && element.scrollHeight > element.clientHeight + 1)
}

async function loadMap({ silent = false } = {}) {
  const requestId = ++latestLoadRequest
  const careerId = route.params.careerId
  if (!silent) loading.value = true
  loadError.value = ''
  try {
    const data = await getCareerNebulaMap()
    const nextCareers = Array.isArray(data.careers) ? data.careers : []
    let nextSkills = Array.isArray(data.skills) ? data.skills : []
    const nextEdges = Array.isArray(data.edges) ? data.edges : []
    if (careerId) {
      const exploration = await getCareerExploration(careerId)
      const summaries = new Map((exploration.planets || []).map((planet) => [planet.id, planet]))
      nextSkills = nextSkills.map((skill) => summaries.has(skill.id)
        ? { ...skill, ...summaries.get(skill.id) }
        : skill)
    } else {
      const explorations = await Promise.all(nextCareers.filter((career) => career.status === 'enabled')
        .map((career) => getCareerExploration(career.id).catch(() => ({ planets: [] }))))
      const summaries = new Map(explorations.flatMap((item) => item.planets || []).map((planet) => [planet.id, planet]))
      nextSkills = nextSkills.map((skill) => summaries.has(skill.id) ? { ...skill, ...summaries.get(skill.id) } : skill)
    }
    if (requestId !== latestLoadRequest || careerId !== route.params.careerId) return
    careers.value = nextCareers
    skills.value = nextSkills
    edges.value = nextEdges
    if (selectedSkill.value) {
      selectedSkill.value = nextSkills.find((skill) => skill.id === selectedSkill.value.id) || null
    }
    if (careerId) {
      const hasLearningPlanets = nextSkills.some(
        (skill) => (skill.careerId || 'testing') === careerId && skill.status === 'enabled',
      )
      if (!hasLearningPlanets) {
        const careersWithPlanets = nextCareers.filter((career) => career.status === 'enabled'
          && nextSkills.some(
            (skill) => (skill.careerId || 'testing') === career.id && skill.status === 'enabled',
          ))
        if (careersWithPlanets.length === 1 && careersWithPlanets[0].id !== careerId) {
          await router.replace({
            name: 'career-nebula',
            params: { careerId: careersWithPlanets[0].id },
          })
        }
      }
    }
  } catch (error) {
    if (requestId !== latestLoadRequest) return
    loadError.value = error.message || '岗位星图暂时无法打开'
  } finally {
    if (requestId === latestLoadRequest && !silent) loading.value = false
  }
}

function refreshWhenActive() {
  if (document.hidden) return
  const now = Date.now()
  if (now - lastAutomaticRefreshAt < 300) return
  lastAutomaticRefreshAt = now
  loadMap({ silent: true })
}

watch(() => route.params.careerId, () => {
  selectedSkill.value = null
  loadMap()
})

watch(selectedCareer, async () => {
  descriptionOverflow.value = false
  await nextTick()
  checkDescriptionOverflow()
})

onMounted(() => {
  loadMap()
  window.addEventListener('resize', checkDescriptionOverflow)
  window.addEventListener('focus', refreshWhenActive)
  document.addEventListener('visibilitychange', refreshWhenActive)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', checkDescriptionOverflow)
  window.removeEventListener('focus', refreshWhenActive)
  document.removeEventListener('visibilitychange', refreshWhenActive)
})
</script>

<template>
  <div class="nebula-page">
    <AppTabBar />

    <main class="nebula-shell">
      <header v-if="!isLearningGalaxy" class="nebula-hero">
        <div class="nebula-hero__intro">
          <p>CAREER CONSTELLATION</p>
          <h1>星图探索</h1>
          <span>沿着岗位与能力路径，发现下一站成长方向。</span>
          <div class="nebula-hero__status">
            <i aria-hidden="true"></i>
            <span>学习数据已连接</span>
          </div>
        </div>
        <article class="python-entry">
          <p class="python-entry__eyebrow">PYTHON PATH</p>
          <h2 class="python-entry__title">进入 Python 学习空间</h2>
          <p class="python-entry__desc">课程、练习、知识图谱</p>
          <RouterLink class="python-entry__action" to="/career/nebula/python">进入 Python 空间</RouterLink>
        </article>
      </header>

      <section v-if="!loading && !loadError && !isLearningGalaxy" class="nebula-metrics" aria-label="学习概览">
        <article>
          <span>开放岗位</span>
          <strong>{{ enabledCareers.length }}</strong>
          <small>条可探索方向</small>
        </article>
        <article>
          <span>学习星球</span>
          <strong>{{ configuredSkills.length }}<em>/{{ enabledSkills.length }}</em></strong>
          <small>已关联课程内容</small>
        </article>
        <article>
          <span>章节进度</span>
          <strong>{{ learningSummary.completedChapters }}<em>/{{ learningSummary.totalChapters }}</em></strong>
          <small>来自真实学习记录</small>
        </article>
        <article>
          <span>平均探索度</span>
          <strong>{{ learningSummary.averageProgress }}<em>%</em></strong>
          <small>全部岗位综合进度</small>
        </article>
      </section>

      <section v-if="!loading && !loadError && !isLearningGalaxy" class="nebula-panels" aria-label="学习任务与统计">
        <article class="nebula-panel nebula-panel--tasks">
          <header class="nebula-panel__head">
            <div>
              <small>LEARNING TASKS</small>
              <h2>继续学习</h2>
            </div>
            <span>{{ inProgressSkills.length }} 个进行中</span>
          </header>
          <div v-if="taskSkills.length" class="task-list">
            <button
              v-for="skill in taskSkills"
              :key="skill.id"
              class="task-card"
              type="button"
              @click="openTaskSkill(skill)"
            >
              <span class="task-planet" :style="skill.image ? { backgroundImage: `url(${skill.image})` } : {}">
                <span v-if="!skill.image">{{ monogram(skill.name) }}</span>
              </span>
              <span class="task-copy">
                <strong>{{ skill.name }}</strong>
                <small>{{ careerNameOf(skill) }} · {{ skillProgress(skill).completed }}/{{ skillProgress(skill).total }} 章节</small>
                <i class="progress-track"><i :style="{ width: `${skillProgress(skill).percentage}%` }"></i></i>
              </span>
              <em>{{ skillProgress(skill).percentage }}%</em>
            </button>
          </div>
          <p v-else class="task-empty">暂无可继续的学习星球，先从一个岗位星系开始探索。</p>
        </article>

        <article class="nebula-panel nebula-panel--chart">
          <header class="nebula-panel__head">
            <div>
              <small>CHAPTER COMPLETION</small>
              <h2>章节完成度</h2>
            </div>
            <span>{{ learningSummary.completedChapters }}/{{ learningSummary.totalChapters }}</span>
          </header>
          <div class="chapter-ring" :style="{ '--value': chapterPercent }">
            <span>{{ chapterPercent }}<em>%</em></span>
          </div>
          <p class="chapter-hint">全部学习星球的章节完成比例，数据来自学习记录。</p>
        </article>

        <article class="nebula-panel nebula-panel--progress">
          <header class="nebula-panel__head">
            <div>
              <small>CAREER PROGRESS</small>
              <h2>岗位进度</h2>
            </div>
            <span>{{ enabledCareers.length }} 个岗位</span>
          </header>
          <ul class="career-progress">
            <li v-for="row in careerProgressRows" :key="row.id">
              <span class="career-progress__name">{{ row.name }}</span>
              <i class="progress-track"><i :style="{ width: `${row.percentage}%` }"></i></i>
              <em>{{ row.percentage }}%</em>
            </li>
          </ul>
        </article>
      </section>

      <div v-if="loading" class="center-message">
        <span class="loading-ring" aria-hidden="true"></span>
        正在打开岗位星图
      </div>

      <div v-else-if="loadError" class="center-message center-message--error">
        <p>{{ loadError }}</p>
        <button type="button" @click="loadMap">重新加载</button>
      </div>

      <template v-else-if="!isLearningGalaxy">
        <section class="career-layout" :class="{ 'career-layout--selected': selectedCareer }">
          <aside class="side-panel career-list-panel">
            <div class="panel-heading">
              <div>
                <small>CAREER NEBULA</small>
                <h2>岗位星云</h2>
              </div>
              <span>{{ visibleCareers.length }}</span>
            </div>

            <label class="career-search">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-4-4" />
              </svg>
              <input
                v-model="searchQuery"
                type="search"
                placeholder="搜索岗位或方向"
                aria-label="搜索岗位"
              />
            </label>

            <div class="career-list">
              <button
                v-for="(career, index) in visibleCareers"
                :key="career.id"
                class="career-list-item"
                :class="{ active: selectedCareerId === career.id }"
                type="button"
                @click="selectCareer(career)"
              >
                <span class="item-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="item-copy">
                  <strong>{{ career.name }}</strong>
                  <small>探索进度 {{ careerProgress(career).percentage }}%</small>
                </span>
                <span class="mini-nebula" :style="career.image ? { backgroundImage: `url(${career.image})` } : {}">
                  <span v-if="!career.image">{{ monogram(career.name) }}</span>
                </span>
              </button>
            </div>
          </aside>

          <section class="map-panel career-map" aria-label="岗位星图">
            <div class="map-grid" aria-hidden="true"></div>
            <div class="orbit orbit--one" aria-hidden="true"></div>
            <div class="orbit orbit--two" aria-hidden="true"></div>
            <button
              v-for="career in visibleCareers"
              :key="career.id"
              class="career-node"
              :class="{ active: selectedCareerId === career.id, muted: selectedCareer && selectedCareerId !== career.id }"
              :style="nodeStyle(career, Boolean(selectedCareer))"
              type="button"
              :aria-label="`查看${career.name}`"
              @click="selectCareer(career)"
            >
              <span class="node-image" :style="career.image ? { backgroundImage: `url(${career.image})` } : {}">
                <span v-if="!career.image">{{ monogram(career.name) }}</span>
              </span>
              <span class="node-label">
                <strong>{{ career.name }}</strong>
                <small>探索进度 {{ careerProgress(career).percentage }}%</small>
                <i class="node-progress"><i :style="{ width: `${careerProgress(career).percentage}%` }"></i></i>
              </span>
            </button>
          </section>

          <Transition name="detail-panel">
            <aside v-if="selectedCareer" class="side-panel career-detail">
              <button class="panel-close" type="button" aria-label="关闭岗位介绍" @click="closeCareerDetail">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12" /></svg>
              </button>
              <small class="detail-kicker">CAREER PROFILE</small>
              <div
                class="detail-image"
                :style="selectedCareer.image ? { backgroundImage: `url(${selectedCareer.image})` } : {}"
              >
                <span v-if="!selectedCareer.image">{{ monogram(selectedCareer.name) }}</span>
              </div>
              <h2>{{ selectedCareer.name }}</h2>
              <div class="detail-description-row">
                <p ref="detailDescription" class="detail-description">{{ selectedCareer.description }}</p>
                <button
                  v-if="descriptionOverflow"
                  class="description-more"
                  type="button"
                  aria-label="查看完整岗位介绍"
                  @click="showFullCareerDescription = true"
                >&gt;&gt;</button>
              </div>
              <div class="detail-progress-summary">
                <div>
                  <span>探索进度</span>
                  <strong>{{ careerProgress(selectedCareer).percentage }}%</strong>
                </div>
                <div class="progress-track">
                  <span :style="{ width: `${careerProgress(selectedCareer).percentage}%` }"></span>
                </div>
              </div>
              <div class="planet-progress-section">
                <h3>星球探索</h3>
                <div class="planet-progress-list">
                  <div v-for="skill in enabledSkillsForCareer(selectedCareer.id)" :key="skill.id" class="planet-progress-item">
                    <span class="progress-planet" :style="skill.image ? { backgroundImage: `url(${skill.image})` } : {}">
                      <span v-if="!skill.image">{{ monogram(skill.name) }}</span>
                    </span>
                    <span class="progress-copy">
                      <strong>{{ skill.name }}</strong>
                      <small>{{ skillProgress(skill).completed }} / {{ skillProgress(skill).total }}</small>
                      <i class="progress-track"><i :style="{ width: `${skillProgress(skill).percentage}%` }"></i></i>
                    </span>
                    <em>{{ skillProgress(skill).percentage }}%</em>
                  </div>
                </div>
              </div>
              <button class="enter-button" type="button" @click="enterLearningGalaxy(selectedCareer)">
                进入学习星系
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
              </button>
            </aside>
          </Transition>

          <Transition name="description-popover">
            <section
              v-if="selectedCareer && showFullCareerDescription"
              class="career-description-popover"
              role="dialog"
              aria-modal="true"
              aria-label="完整岗位介绍"
            >
              <button class="panel-close" type="button" aria-label="关闭完整岗位介绍" @click="showFullCareerDescription = false">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12" /></svg>
              </button>
              <small>CAREER DESCRIPTION</small>
              <h2>{{ selectedCareer.name }}</h2>
              <p>{{ selectedCareer.description }}</p>
            </section>
          </Transition>
        </section>
      </template>

      <template v-else-if="activeCareer">
        <header class="learning-header">
          <h1>{{ activeCareer.name }} · 学习星系</h1>
          <button type="button" @click="returnToCareerMap">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg>
            返回岗位星图
          </button>
        </header>

        <section class="learning-layout">
          <aside class="side-panel skill-list-panel">
            <div class="panel-heading">
              <div>
                <small>LEARNING PLANETS</small>
                <h2>学习星球</h2>
              </div>
              <span>{{ activeCareerSkills.length }}</span>
            </div>
            <div class="skill-list">
              <button
                v-for="(skill, index) in activeCareerSkills"
                :key="skill.id"
                class="skill-list-item"
                :class="{ active: selectedSkill?.id === skill.id }"
                type="button"
                @click="openSkill(skill)"
              >
                <span class="item-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="item-copy">
                  <strong>{{ skill.name }}</strong>
                  <small>探索进度 {{ skillProgress(skill).percentage }}%</small>
                </span>
                <span class="mini-nebula" :style="skill.image ? { backgroundImage: `url(${skill.image})` } : {}">
                  <span v-if="!skill.image">{{ monogram(skill.name) }}</span>
                </span>
              </button>
            </div>
          </aside>

          <section class="map-panel learning-map" aria-label="岗位学习星系">
            <div v-if="!activeCareerSkills.length" class="center-message center-message--error">
              <p>该岗位尚未配置学习星球，请返回岗位星图选择已配置的岗位。</p>
              <button type="button" @click="returnToCareerMap">返回岗位星图</button>
            </div>
            <div class="map-grid" aria-hidden="true"></div>
            <svg class="edge-layer" preserveAspectRatio="none" aria-hidden="true">
              <defs>
                <marker id="route-arrow-main" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                  <path d="M 1 1 L 9 5 L 1 9 L 3.5 5 Z" />
                </marker>
                <marker id="route-arrow-branch" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                  <path d="M 1 1 L 9 5 L 1 9 L 3.5 5 Z" />
                </marker>
              </defs>
              <template v-for="edge in activeEdges" :key="edge.id">
                <g v-if="edgeLine(edge)" class="route-edge" :class="{ branch: edge.type === '支线' }">
                  <line class="edge-glow" v-bind="edgeLine(edge)" />
                  <line class="edge-flow" v-bind="edgeLine(edge)" />
                </g>
              </template>
            </svg>

            <button
              v-for="skill in activeCareerSkills"
              :key="skill.id"
              class="skill-node"
              :class="{ active: selectedSkill?.id === skill.id }"
              :style="nodeStyle(skill)"
              type="button"
              @click="openSkill(skill)"
            >
              <span class="node-image" :style="skill.image ? { backgroundImage: `url(${skill.image})` } : {}">
                <span v-if="!skill.image">{{ monogram(skill.name) }}</span>
              </span>
              <span class="node-label">
                <strong>{{ skill.name }}</strong>
                <small>探索进度 {{ skillProgress(skill).percentage }}%</small>
                <i class="node-progress"><i :style="{ width: `${skillProgress(skill).percentage}%` }"></i></i>
              </span>
            </button>
          </section>
        </section>
      </template>

      <div v-else class="center-message center-message--error">
        <p>没有找到对应的岗位学习星系</p>
        <button type="button" @click="returnToCareerMap">返回岗位星图</button>
      </div>
    </main>

    <Transition name="modal-fade">
      <CareerPlanetView
        v-if="selectedSkill?.configured && activeCareer"
        :career-id="activeCareer.id"
        :skill-id="selectedSkill.id"
        modal
        @close="closeSkill"
      />
    </Transition>

    <Transition name="modal-fade">
      <div v-if="selectedSkill && !selectedSkill.configured" class="modal-backdrop" role="presentation" @click.self="closeSkill">
        <section class="learning-modal" role="dialog" aria-modal="true" :aria-label="`${selectedSkill.name}学习内容`">
          <button class="panel-close" type="button" aria-label="关闭学习内容" @click="closeSkill">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12" /></svg>
          </button>
          <small>LEARNING CONTENT</small>
          <h2>{{ selectedSkill.name }} · 学习内容</h2>
          <div v-if="!selectedSkill.configured" class="content-empty">
            {{ selectedSkill.configurationReason || '该星球尚未关联已发布课程，请先在岗位星图管理中配置课程。' }}
          </div>
          <div v-else class="content-empty">课程内容将从关联课程自动读取。</div>
          <button class="modal-close-button" type="button" @click="closeSkill">关闭</button>
        </section>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.nebula-page {
  height: 100vh;
  overflow: hidden;
  color: #dcecff;
  background:
    radial-gradient(circle at 22% 24%, rgba(16, 87, 153, 0.2), transparent 26%),
    radial-gradient(circle at 76% 32%, rgba(65, 53, 132, 0.18), transparent 28%),
    linear-gradient(160deg, #020812 0%, #061221 54%, #020711 100%);
}

.nebula-page::before {
  position: fixed;
  inset: 60px 0 0;
  pointer-events: none;
  content: '';
  opacity: 0.34;
  background-image:
    radial-gradient(circle, rgba(150, 210, 255, 0.8) 0 1px, transparent 1.5px),
    radial-gradient(circle, rgba(255, 255, 255, 0.55) 0 1px, transparent 1.4px);
  background-position: 0 0, 43px 59px;
  background-size: 83px 83px, 127px 127px;
}

.nebula-shell {
  position: relative;
  z-index: 1;
  width: min(1680px, calc(100% - 40px));
  height: 100vh;
  margin: 0 auto;
  padding: 70px 0 12px;
}

.nebula-hero {
  display: flex;
  align-items: center;
  height: 42px;
  margin: 0 0 8px;
  padding-left: 2px;
  text-align: left;
}

.nebula-hero p,
.learning-header p,
.detail-kicker,
.learning-modal > small {
  margin: 0 0 5px;
  color: #58bfff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.28em;
}

.nebula-hero h1,
.learning-header h1 {
  margin: 0;
  color: #f2f8ff;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-shadow: 0 0 24px rgba(65, 174, 255, 0.28);
}

.nebula-hero h1 { font-size: 22px; letter-spacing: 0.06em; }
.nebula-hero span,
.learning-header span { color: #7f9bb5; font-size: 14px; }

.career-layout,
.learning-layout {
  display: grid;
  gap: 16px;
  min-height: 0;
}

.career-layout {
  position: relative;
  height: calc(100vh - 132px);
  grid-template-columns: 236px minmax(0, 1fr);
}
.career-layout--selected { grid-template-columns: 220px minmax(0, 1fr) 340px; }
.learning-layout {
  height: calc(100vh - 152px);
  grid-template-columns: 268px minmax(0, 1fr);
}

.side-panel,
.map-panel,
.learning-header {
  border: 1px solid rgba(49, 153, 219, 0.4);
  background: rgba(3, 15, 29, 0.82);
  box-shadow: inset 0 0 32px rgba(14, 67, 107, 0.1);
}

.side-panel {
  position: relative;
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}
.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 82px;
  padding: 18px;
  border-bottom: 1px solid rgba(49, 153, 219, 0.34);
}
.panel-heading small { color: #4c84a7; font-size: 10px; letter-spacing: 0.18em; }
.panel-heading h2 { margin: 2px 0 0; color: #dcecff; font-size: 19px; font-weight: 600; }
.panel-heading > span { color: #5ba7d8; font-size: 12px; }

.career-list,
.skill-list {
  display: grid;
  min-height: 0;
  padding: 12px;
  gap: 9px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-color: rgba(65, 156, 211, 0.58) rgba(4, 20, 35, 0.45);
  scrollbar-width: thin;
}
.career-list::-webkit-scrollbar,
.skill-list::-webkit-scrollbar { width: 6px; }
.career-list::-webkit-scrollbar-track,
.skill-list::-webkit-scrollbar-track { background: rgba(4, 20, 35, 0.45); }
.career-list::-webkit-scrollbar-thumb,
.skill-list::-webkit-scrollbar-thumb { background: rgba(65, 156, 211, 0.58); }
.career-list-item,
.skill-list-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 46px;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 78px;
  padding: 9px 10px;
  border: 1px solid rgba(48, 137, 195, 0.34);
  color: #a8c0d2;
  background: rgba(4, 22, 40, 0.66);
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}
.career-list-item:hover,
.career-list-item.active,
.skill-list-item:hover,
.skill-list-item.active {
  border-color: #35aeef;
  color: #edf8ff;
  background: rgba(12, 75, 116, 0.46);
}
.item-index { color: #44baff; font-size: 16px; }
.item-copy { min-width: 0; }
.item-copy strong,
.item-copy small { display: block; }
.item-copy strong { overflow: hidden; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.item-copy small {
  display: -webkit-box;
  margin-top: 3px;
  overflow: hidden;
  color: #66859d;
  font-size: 10px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.mini-nebula {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid #318bc1;
  border-radius: 50%;
  color: #90d8ff;
  background: radial-gradient(circle at 35% 30%, #278fd0, #0a3152 50%, #040d18 72%);
  background-position: center;
  background-size: cover;
  box-shadow: 0 0 14px rgba(39, 157, 226, 0.26);
  font-size: 11px;
}
.mini-nebula svg { width: 17px; fill: none; stroke: currentColor; stroke-width: 1.6; }

.map-panel { position: relative; min-width: 0; min-height: 0; overflow: hidden; }
.map-grid {
  position: absolute;
  inset: 0;
  opacity: 0.28;
  background-image:
    linear-gradient(rgba(37, 115, 165, 0.22) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 115, 165, 0.22) 1px, transparent 1px);
  background-size: 64px 64px;
}
.orbit {
  position: absolute;
  left: 50%;
  top: 50%;
  border: 1px solid rgba(74, 165, 223, 0.16);
  border-radius: 50%;
  transform: translate(-50%, -50%) rotate(-8deg);
}
.orbit--one { width: 62%; height: 54%; }
.orbit--two { width: 86%; height: 76%; }
.career-node,
.skill-node {
  position: absolute;
  z-index: 2;
  display: grid;
  place-items: center;
  padding: 0;
  color: #dcedfa;
  background: transparent;
  transform: translate(-50%, -50%);
  transition: width 0.28s ease, height 0.28s ease, opacity 0.22s ease, filter 0.22s ease;
}
.node-image {
  display: grid;
  place-items: center;
  width: 100%;
  aspect-ratio: 1;
  border: 1px solid rgba(85, 192, 255, 0.72);
  border-radius: 50%;
  color: #e1f5ff;
  background: radial-gradient(circle at 34% 28%, #4bb9f4 0, #136096 26%, #0a2947 54%, #020a13 74%);
  background-position: center;
  background-size: cover;
  box-shadow: 0 0 20px rgba(49, 171, 239, 0.36), inset -10px -13px 22px rgba(0, 0, 0, 0.5);
  font-size: 20px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.career-node:hover .node-image,
.career-node.active .node-image,
.skill-node:hover .node-image,
.skill-node.active .node-image {
  box-shadow: 0 0 34px rgba(66, 193, 255, 0.74), 0 0 0 4px rgba(48, 168, 232, 0.16);
  transform: scale(1.04);
}
.career-node.muted { opacity: 0.58; }
.node-label {
  position: absolute;
  top: calc(100% + 10px);
  left: 50%;
  min-width: 130px;
  padding: 7px 10px;
  border-left: 2px solid #39b9ff;
  color: #dbefff;
  background: rgba(2, 13, 24, 0.86);
  font-size: 12px;
  text-align: left;
  transform: translateX(-50%);
}
.node-label strong,
.node-label small { display: block; }
.node-label strong { font-size: 12px; font-weight: 600; }
.node-label small { margin-top: 2px; color: #64839b; font-size: 10px; }
.node-progress { display: block; width: 100%; height: 3px; margin-top: 5px; overflow: hidden; background: rgba(63, 132, 174, 0.25); }
.node-progress > i { display: block; height: 100%; background: #3ab9ff; box-shadow: 0 0 7px rgba(58, 185, 255, 0.7); transition: width 0.25s ease; }

.career-detail { padding: 22px 20px 20px; }
.panel-close {
  display: grid;
  place-items: center;
  position: absolute;
  z-index: 3;
  top: 16px;
  right: 16px;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(63, 161, 221, 0.48);
  color: #9dd9fa;
  background: rgba(4, 24, 42, 0.86);
}
.panel-close svg { width: 18px; fill: none; stroke: currentColor; stroke-width: 1.8; }
.detail-image {
  display: grid;
  place-items: center;
  width: calc(100% + 20px);
  height: clamp(135px, 21vh, 180px);
  margin: 38px -10px 16px;
  border: 1px solid rgba(78, 190, 255, 0.65);
  border-radius: 4px;
  color: #e4f6ff;
  background: radial-gradient(circle at 35% 30%, #389ed8, #123e67 50%, #030b15 74%);
  background-position: center;
  background-size: cover;
  box-shadow: 0 0 34px rgba(42, 160, 224, 0.32);
  font-size: 25px;
}
.career-detail h2 {
  margin: 0 0 10px;
  overflow: hidden;
  color: #f1f8ff;
  font-size: 21px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-description-row {
  position: relative;
  min-height: 48px;
  padding-right: 28px;
}
.detail-description {
  display: -webkit-box;
  max-height: 48px;
  margin: 0;
  overflow: hidden;
  overflow-wrap: anywhere;
  color: #82a0b6;
  font-size: 13px;
  line-height: 24px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.description-more {
  position: absolute;
  right: 0;
  bottom: 0;
  padding: 0 2px;
  color: #54c3ff;
  background: #03101e;
  font-size: 13px;
  font-weight: 700;
}
.detail-progress-summary { margin-top: 14px; padding: 12px 0; border-top: 1px solid rgba(48, 137, 195, 0.24); border-bottom: 1px solid rgba(48, 137, 195, 0.24); }
.detail-progress-summary > div:first-child { display: flex; align-items: center; justify-content: space-between; color: #89a8bd; font-size: 13px; }
.detail-progress-summary strong { color: #eaf7ff; font-size: 22px; font-weight: 650; }
.progress-track { display: block; height: 4px; margin-top: 7px; overflow: hidden; background: rgba(66, 132, 173, 0.2); }
.progress-track > span,
.progress-track > i { display: block; height: 100%; background: #3db9ff; box-shadow: 0 0 8px rgba(61, 185, 255, 0.7); transition: width 0.25s ease; }
.planet-progress-section { display: flex; min-height: 0; margin: 12px 0; flex: 1; flex-direction: column; }
.planet-progress-section h3 { margin: 0 0 8px; color: #8ba9bd; font-size: 12px; font-weight: 600; }
.planet-progress-list { display: grid; min-height: 0; gap: 7px; overflow-y: auto; scrollbar-color: rgba(65, 156, 211, 0.58) transparent; scrollbar-width: thin; }
.planet-progress-item { display: grid; grid-template-columns: 34px minmax(0, 1fr) auto; align-items: center; gap: 8px; padding: 6px 8px; border: 1px solid rgba(48, 137, 195, 0.24); background: rgba(5, 28, 48, 0.58); }
.progress-planet { display: grid; place-items: center; width: 32px; height: 32px; border: 1px solid rgba(64, 174, 235, 0.55); border-radius: 50%; color: #bce9ff; background: radial-gradient(circle at 35% 30%, #278fd0, #0a3152 55%, #040d18 75%); background-position: center; background-size: cover; font-size: 9px; }
.progress-copy { min-width: 0; }
.progress-copy strong,
.progress-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.progress-copy strong { color: #dceefa; font-size: 11px; font-weight: 600; }
.progress-copy small { color: #638298; font-size: 9px; }
.progress-copy .progress-track { height: 3px; margin-top: 4px; }
.planet-progress-item > em { color: #67c9ff; font-size: 11px; font-style: normal; }
.enter-button,
.modal-close-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  min-height: 48px;
  border: 1px solid #43baf5;
  color: #eefaff;
  background: #106ca8;
  font-weight: 600;
}
.enter-button:hover,
.modal-close-button:hover { background: #1480c4; }
.enter-button svg { width: 19px; fill: none; stroke: currentColor; stroke-width: 1.8; }

.career-description-popover {
  position: absolute;
  z-index: 12;
  top: 50%;
  right: 356px;
  width: min(540px, calc(100% - 590px));
  max-height: calc(100% - 48px);
  padding: 32px 34px;
  overflow-y: auto;
  border: 1px solid #299fdf;
  color: #cfe7f6;
  background: rgba(5, 24, 42, 0.98);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.48), inset 0 0 36px rgba(23, 102, 153, 0.1);
  transform: translateY(-50%);
}
.career-description-popover > small { color: #55c1fb; font-size: 11px; letter-spacing: 0.22em; }
.career-description-popover h2 { margin: 10px 42px 18px 0; color: #f0f8ff; font-size: 24px; font-weight: 650; }
.career-description-popover p {
  margin: 0;
  overflow-wrap: anywhere;
  color: #96b2c6;
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.learning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  height: 60px;
  min-height: 60px;
  margin-bottom: 10px;
  padding: 9px 18px;
}
.learning-header h1 { font-size: 21px; letter-spacing: 0.04em; }
.learning-header button {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 17px;
  border: 1px solid rgba(64, 177, 238, 0.58);
  color: #a8daf5;
  background: rgba(8, 40, 67, 0.7);
}
.learning-header button svg { width: 18px; fill: none; stroke: currentColor; stroke-width: 1.8; }
.edge-layer { position: absolute; inset: 0; z-index: 1; width: 100%; height: 100%; pointer-events: none; }
.edge-layer marker path { fill: #7ddcff; filter: drop-shadow(0 0 2px rgba(56, 189, 248, 0.95)); }
.edge-layer #route-arrow-branch path { fill: #c4a8ff; filter: drop-shadow(0 0 2px rgba(167, 139, 250, 0.95)); }
.route-edge line { vector-effect: non-scaling-stroke; stroke-linecap: round; }
.route-edge .edge-glow {
  stroke: #168dd0;
  stroke-width: 5;
  opacity: 0.22;
  filter: blur(1.8px);
}
.route-edge .edge-flow {
  stroke: #38bdf8;
  stroke-width: 2;
  stroke-dasharray: 8 7;
  marker-end: url(#route-arrow-main);
  filter: drop-shadow(0 0 4px rgba(56, 189, 248, 0.78));
  animation: route-flow 2.1s linear infinite;
}
.route-edge.branch .edge-glow { stroke: #7658c8; opacity: 0.18; }
.route-edge.branch .edge-flow {
  stroke: #a78bfa;
  stroke-dasharray: 4 8;
  marker-end: url(#route-arrow-branch);
  filter: drop-shadow(0 0 4px rgba(167, 139, 250, 0.68));
}

.modal-backdrop {
  position: fixed;
  z-index: 1600;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(0, 5, 12, 0.78);
  backdrop-filter: blur(6px);
}
.learning-modal {
  position: relative;
  width: min(780px, 100%);
  max-height: min(720px, calc(100vh - 48px));
  overflow-y: auto;
  padding: 38px 40px 32px;
  border: 1px solid #279cdc;
  color: #cfe5f5;
  background: #06172a;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.5), inset 0 0 42px rgba(20, 97, 147, 0.1);
}
.learning-modal h2 { margin: 4px 0 24px; color: #f0f9ff; font-size: 25px; font-weight: 650; }
.content-list { display: grid; gap: 10px; }
.content-item {
  display: grid;
  grid-template-columns: 45px minmax(0, 1fr) auto 118px;
  align-items: center;
  min-height: 64px;
  padding: 10px 14px;
  border: 1px solid rgba(48, 143, 201, 0.44);
  background: rgba(7, 35, 59, 0.74);
}
.content-item > span { color: #42b9fb; font-size: 18px; }
.content-item strong { color: #d9ecf8; font-size: 14px; font-weight: 600; }
.content-item em { padding: 5px 12px; border-radius: 999px; font-size: 12px; font-style: normal; }
.content-item select { width: 118px; min-height: 38px; padding: 0 30px 0 12px; border: 1px solid rgba(55, 158, 217, 0.58); border-radius: 0; outline: none; color: #cbeaff; background: #071a2e; }
.content-item select:disabled { cursor: wait; opacity: 0.62; }
.progress-error { margin: 12px 0 0; color: #fca5a5; font-size: 12px; }
.type-知识 { color: #7dd3fc; background: rgba(14, 116, 144, 0.3); }
.type-实践 { color: #86efac; background: rgba(21, 128, 61, 0.28); }
.type-考核 { color: #fdba74; background: rgba(194, 65, 12, 0.26); }
.content-empty { padding: 48px 20px; border: 1px dashed rgba(65, 144, 194, 0.38); color: #63839a; text-align: center; }
.modal-close-button { width: 150px; min-height: 42px; margin: 24px 0 0 auto; }

.center-message {
  display: grid;
  place-items: center;
  align-content: center;
  min-height: calc(100vh - 116px);
  gap: 14px;
  color: #7ba3bd;
}
.center-message p { margin: 0; }
.center-message button { min-height: 40px; padding: 0 18px; color: #bde7ff; background: #0c6095; }
.loading-ring { width: 34px; height: 34px; border: 2px solid rgba(86, 178, 231, 0.2); border-top-color: #55c2ff; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes route-flow { to { stroke-dashoffset: -30; } }
@keyframes spin { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) {
  .route-edge .edge-flow { animation: none; }
}

.detail-panel-enter-active,
.detail-panel-leave-active { transition: opacity 0.24s ease, transform 0.24s ease; }
.detail-panel-enter-from,
.detail-panel-leave-to { opacity: 0; transform: translateX(18px); }
.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }
.description-popover-enter-active,
.description-popover-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.description-popover-enter-from,
.description-popover-leave-to { opacity: 0; transform: translate(18px, -50%); }

@media (max-width: 1120px) {
  .career-layout--selected { grid-template-columns: 205px minmax(0, 1fr) 300px; }
  .career-layout--selected .career-node { transform: translate(-50%, -50%) scale(0.86); }
  .learning-layout { grid-template-columns: 235px minmax(0, 1fr); }
}

@media (max-width: 860px) {
  .nebula-page { height: auto; min-height: 100vh; overflow: visible; }
  .nebula-shell { width: min(100% - 24px, 760px); height: auto; min-height: 100vh; }
  .career-layout,
  .career-layout--selected,
  .learning-layout { height: auto; min-height: 0; grid-template-columns: 1fr; }
  .career-list-panel,
  .skill-list-panel { max-height: 250px; overflow-y: auto; }
  .career-detail { position: fixed; z-index: 20; right: 12px; bottom: 12px; left: 12px; max-height: 58vh; overflow-y: auto; }
  .detail-image { width: calc(100% + 12px); height: 150px; margin: 35px -6px 12px; }
  .map-panel { min-height: 590px; }
  .career-description-popover { position: fixed; z-index: 24; top: 50%; right: 18px; left: 18px; width: auto; max-height: 70vh; }
  .learning-header { align-items: center; }
}

@media (max-width: 560px) {
  .nebula-shell { padding-top: 78px; }
  .nebula-hero h1 { font-size: 28px; }
  .map-panel { min-height: 520px; }
  .career-node,
  .skill-node { transform: translate(-50%, -50%) scale(0.72); }
  .learning-modal { padding: 32px 20px 22px; }
  .content-item { grid-template-columns: 38px minmax(0, 1fr); }
  .content-item em { grid-column: 2; justify-self: start; margin-top: 4px; }
  .content-item select { grid-column: 2; width: 100%; margin-top: 4px; }
}
/* Editorial SaaS / retro pastel refresh */
.nebula-page {
  --ink: #24231f;
  --muted: #736f67;
  --paper: #f3efe6;
  --surface: #fffdf7;
  --pink: #ead5d8;
  --olive: #b7c3a2;
  --blue: #bdcbd2;
  color: var(--ink);
  background: var(--paper);
}
.nebula-page::before {
  inset: 60px 0 0;
  opacity: .32;
  background-image: radial-gradient(circle, #bdb6aa 0 1px, transparent 1.2px);
  background-position: 12px 12px;
  background-size: 28px 28px;
}
.nebula-shell { width: min(1680px, calc(100% - 48px)); padding-top: 74px; }
.editorial-toolbar { margin-bottom: 14px; }
.nebula-hero { height: 94px; margin-bottom: 14px; padding: 0 8px; }
.nebula-hero p,
.learning-header p,
.detail-kicker,
.learning-modal > small { color: #787268; letter-spacing: .18em; }
.nebula-hero h1,
.learning-header h1 { color: var(--ink); font-size: clamp(30px, 3vw, 46px); font-weight: 800; letter-spacing: -.045em; text-shadow: none; }
.nebula-hero span,
.learning-header span { display: block; margin-top: 5px; color: var(--muted); font-size: 13px; }
.career-layout { height: calc(100vh - 272px); grid-template-columns: 252px minmax(0, 1fr); }
.career-layout--selected { grid-template-columns: 236px minmax(0, 1fr) 342px; }
.learning-layout { height: calc(100vh - 246px); }
.side-panel,
.map-panel,
.learning-header { border: 1px solid var(--ink); border-radius: 26px; background: rgba(255, 253, 247, .96); box-shadow: none; }
.panel-heading { border-bottom-color: #cfc8bb; }
.panel-heading small { color: #7b756b; }
.panel-heading h2 { color: var(--ink); font-weight: 750; }
.panel-heading > span { color: var(--ink); }
.career-list,
.skill-list { scrollbar-color: #aaa398 #eee7da; }
.career-list-item,
.skill-list-item { border-color: #cbc4b8; border-radius: 18px; color: #4e4b45; background: #f7f2e9; }
.career-list-item:nth-child(3n + 1),
.skill-list-item:nth-child(3n + 1) { background: #f0e1df; }
.career-list-item:nth-child(3n + 2),
.skill-list-item:nth-child(3n + 2) { background: #e7ecdf; }
.career-list-item:hover,
.career-list-item.active,
.skill-list-item:hover,
.skill-list-item.active { border-color: var(--ink); color: var(--ink); background: #fffdf7; transform: translateY(-1px); }
.item-index { color: #736f67; font-weight: 800; }
.item-copy small { color: #7d786f; }
.mini-nebula { border-color: var(--ink); color: var(--ink); background-color: var(--blue); background-image: none; box-shadow: none; }
.map-panel { background: #e7ded0; }
.map-grid { opacity: .5; background-image: linear-gradient(rgba(60, 57, 52, .11) 1px, transparent 1px), linear-gradient(90deg, rgba(60, 57, 52, .11) 1px, transparent 1px); background-size: 52px 52px; }
.orbit { border-color: rgba(36, 35, 31, .22); }
.node-image { border-color: var(--ink); color: var(--ink); background-image: radial-gradient(circle at 34% 28%, #fff8e8 0, #d9c4d8 34%, #aebfbd 70%, #7e8f8b 100%); box-shadow: 6px 7px 0 rgba(36, 35, 31, .15); }
.career-node:nth-of-type(3n + 1) .node-image,
.skill-node:nth-of-type(3n + 1) .node-image { background-image: radial-gradient(circle at 34% 28%, #fff8e8, #e4c9c9 44%, #b98f91); }
.career-node:nth-of-type(3n + 2) .node-image,
.skill-node:nth-of-type(3n + 2) .node-image { background-image: radial-gradient(circle at 34% 28%, #fff8e8, #d9d7a9 44%, #9fac88); }
.career-node:hover .node-image,
.career-node.active .node-image,
.skill-node:hover .node-image,
.skill-node.active .node-image { box-shadow: 8px 9px 0 rgba(36, 35, 31, .22); transform: translate(-2px, -2px); }
.node-label { color: var(--ink); text-shadow: none; }
.node-label small { color: #5f5b54; }
.node-progress,
.progress-track { background: rgba(36, 35, 31, .14); }
.node-progress i,
.progress-track i,
.progress-track span { background: var(--ink); box-shadow: none; }
.career-detail { color: var(--ink); background: #f3ded9; }
.career-detail h2,
.career-detail h3,
.detail-progress-summary strong,
.planet-progress-item strong { color: var(--ink); }
.detail-description,
.detail-progress-summary span,
.planet-progress-item small { color: #625d55; }
.detail-image,
.progress-planet { border-color: var(--ink); box-shadow: none; }
.panel-close { color: var(--ink); background: rgba(255,255,255,.45); border-color: var(--ink); }
.enter-button,
.center-message button,
.modal-close-button { border-color: var(--ink); border-radius: 999px; color: #fff; background: var(--ink); box-shadow: none; }
.enter-button:hover,
.center-message button:hover,
.modal-close-button:hover { color: #fff; border-color: var(--ink); background: #3a3833; transform: translateY(-1px); }
.career-description-popover,
.learning-modal { border: 1px solid var(--ink); border-radius: 26px; color: var(--ink); background: var(--surface); box-shadow: 10px 12px 0 rgba(36,35,31,.18); }
.career-description-popover h2,
.learning-modal h2 { color: var(--ink); }
.career-description-popover p,
.content-empty { color: #5d5952; }
@media (max-width: 1120px) { .career-layout--selected { grid-template-columns: 210px minmax(0, 1fr) 300px; } }
@media (max-width: 860px) {
  .nebula-shell { width: min(100% - 24px, 760px); padding-top: 72px; }
  .career-layout,
  .career-layout--selected,
  .learning-layout { height: auto; }
  .nebula-hero { height: auto; min-height: 118px; align-items: flex-start; gap: 14px; }
}
/* Deep future dashboard — scoped to Star Map content, the global navbar is untouched. */
.nebula-page {
  --ink: #edf2ff;
  --muted: #8893aa;
  --paper: #070910;
  --surface: #111521;
  --violet: #737cff;
  --blue: #72c7ff;
  --mint: #75ddb9;
  --pink: #ef9eb7;
  --yellow: #ead27b;
  height: auto;
  min-height: 100vh;
  overflow: visible;
  color: var(--ink);
  background:
    radial-gradient(circle at 84% 12%, rgba(104, 82, 220, .18), transparent 28%),
    radial-gradient(circle at 12% 36%, rgba(39, 124, 184, .13), transparent 24%),
    linear-gradient(145deg, #060810 0%, #0a0d16 48%, #070911 100%);
}
.nebula-page::before {
  opacity: .2;
  background-image: radial-gradient(circle, rgba(142, 160, 205, .56) 0 1px, transparent 1.2px);
  background-size: 32px 32px;
}
.nebula-shell {
  width: min(1640px, calc(100% - 48px));
  height: auto;
  min-height: 100vh;
  padding: 74px 0 30px;
}

.career-search {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 12px 0 10px;
  padding: 0 12px;
  border: 1px solid rgba(148, 163, 184, .16);
  border-radius: 999px;
  background: rgba(5, 8, 15, .66);
  transition: border-color .2s ease, background .2s ease;
}

.career-search:focus-within {
  border-color: rgba(124, 137, 255, .72);
  background: rgba(7, 10, 18, .9);
}

.career-search svg {
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
  fill: none;
  stroke: #7c87a3;
  stroke-width: 1.7;
  stroke-linecap: round;
}

.career-search input {
  width: 100%;
  height: 34px;
  border: 0;
  outline: 0;
  color: #eef2fb;
  background: transparent;
  font: inherit;
  font-size: 12px;
}

.career-search input::placeholder { color: #677188; }
.career-search input::-webkit-search-cancel-button { display: none; }

.nebula-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, .72fr);
  height: auto;
  min-height: 184px;
  margin: 0 0 14px;
  padding: 0;
  gap: 14px;
}
.nebula-hero__intro,
.python-entry {
  border: 1px solid rgba(148, 163, 184, .16);
  border-radius: 28px;
  background: rgba(16, 20, 31, .78);
  box-shadow: inset 0 1px 0 rgba(255,255,255,.035), 0 18px 60px rgba(0,0,0,.18);
  backdrop-filter: blur(14px);
}
.nebula-hero__intro {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  padding: 28px 32px;
}
.nebula-hero__intro::after {
  position: absolute;
  right: -58px;
  bottom: -98px;
  width: 290px;
  height: 290px;
  border: 1px solid rgba(126, 137, 255, .26);
  border-radius: 50%;
  box-shadow: 0 0 0 38px rgba(106, 117, 238, .035), 0 0 0 76px rgba(106, 117, 238, .025);
  content: '';
}
.nebula-hero p { color: #75809a; font-size: 10px; letter-spacing: .24em; }
.nebula-hero h1 { margin-top: 8px; color: #f5f7ff; font-size: clamp(34px, 4vw, 58px); letter-spacing: -.06em; }
.nebula-hero__intro > span { max-width: 560px; color: #929db2; font-size: 14px; }
.nebula-hero__status {
  display: flex;
  align-items: center;
  width: max-content;
  margin-top: 20px;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid rgba(117, 221, 185, .2);
  border-radius: 999px;
  background: rgba(69, 154, 126, .09);
}
.nebula-hero__status i { width: 7px; height: 7px; border-radius: 50%; background: var(--mint); box-shadow: 0 0 12px rgba(117,221,185,.72); }
.nebula-hero__status span { margin: 0; color: #8fdabb; font-size: 10px; font-weight: 700; letter-spacing: .04em; }
.python-entry {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 30px 32px;
}
.python-entry p.python-entry__eyebrow {
  margin: 0;
  color: #72c7ff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .22em;
}
.python-entry h2.python-entry__title {
  margin: 12px 0 0;
  color: #f5f7ff;
  font-size: clamp(20px, 1.7vw, 26px);
  font-weight: 700;
  letter-spacing: -.01em;
}
.python-entry p.python-entry__desc {
  margin: 10px 0 0;
  color: #8d98ae;
  font-size: 13px;
  letter-spacing: normal;
}
.python-entry__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: max-content;
  min-height: 44px;
  margin-top: 24px;
  padding: 0 24px;
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, #606af0, #8075e8);
  box-shadow: 0 14px 32px rgba(82, 88, 203, .34);
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}
.python-entry__action:hover {
  color: #fff;
  background: linear-gradient(135deg, #7079fa, #8e84f0);
  box-shadow: 0 18px 38px rgba(96, 102, 224, .42);
  transform: translateY(-1px);
}
.python-entry__action:focus-visible { outline: 2px solid rgba(124, 137, 255, .75); outline-offset: 3px; }
.nebula-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 14px; }
.nebula-metrics article {
  position: relative;
  min-height: 104px;
  overflow: hidden;
  padding: 18px 20px;
  border: 1px solid rgba(148,163,184,.14);
  border-radius: 22px;
  background: rgba(16,20,31,.78);
  box-shadow: inset 0 1px 0 rgba(255,255,255,.03);
}
.nebula-metrics article::after { position: absolute; top: 18px; right: 18px; width: 8px; height: 8px; border-radius: 50%; background: var(--metric-color, var(--blue)); box-shadow: 0 0 16px var(--metric-color, var(--blue)); content: ''; }
.nebula-metrics article:nth-child(2) { --metric-color: var(--pink); }
.nebula-metrics article:nth-child(3) { --metric-color: var(--mint); }
.nebula-metrics article:nth-child(4) { --metric-color: var(--yellow); }
.nebula-metrics span,
.nebula-metrics small { display: block; color: #778298; font-size: 10px; }
.nebula-metrics strong { display: block; margin: 8px 0 4px; color: #f4f6ff; font-size: 27px; line-height: 1; letter-spacing: -.04em; }
.nebula-metrics em { color: #818ba2; font-size: 13px; font-style: normal; }
.career-layout { height: 640px; grid-template-columns: 252px minmax(0, 1fr); }
.career-layout--selected { grid-template-columns: 236px minmax(0, 1fr) 342px; }
.learning-layout { height: calc(100vh - 170px); min-height: 620px; }
.side-panel,
.map-panel,
.learning-header { border-color: rgba(148,163,184,.16); border-radius: 26px; background: rgba(15,19,30,.82); box-shadow: inset 0 1px 0 rgba(255,255,255,.035), 0 18px 48px rgba(0,0,0,.18); backdrop-filter: blur(12px); }
.panel-heading { border-bottom-color: rgba(148,163,184,.12); }
.panel-heading small { color: #626e88; }
.panel-heading h2 { color: #edf2ff; }
.panel-heading > span { color: #7b87a1; }
.career-list,
.skill-list { scrollbar-color: #454d68 transparent; }
.career-list-item,
.skill-list-item,
.career-list-item:nth-child(3n + 1),
.skill-list-item:nth-child(3n + 1),
.career-list-item:nth-child(3n + 2),
.skill-list-item:nth-child(3n + 2) { border-color: rgba(148,163,184,.1); color: #aab4c8; background: rgba(23,28,43,.72); }
.career-list-item:hover,
.career-list-item.active,
.skill-list-item:hover,
.skill-list-item.active { border-color: rgba(124,137,255,.5); color: #f4f6ff; background: linear-gradient(135deg, rgba(83,91,199,.32), rgba(49,44,102,.3)); box-shadow: 0 8px 22px rgba(25,26,63,.28); }
.item-index { color: #707cf5; }
.item-copy small { color: #6e7890; }
.mini-nebula { border-color: rgba(114,199,255,.42); color: #cfeeff; background-image: radial-gradient(circle at 34% 28%, #5eb6dd, #304770 50%, #181c31 76%); box-shadow: 0 0 18px rgba(87,163,225,.18); }
.map-panel { background: radial-gradient(circle at 52% 44%, rgba(52,70,115,.42), transparent 42%), linear-gradient(145deg, #0b0f1a, #0d1321); }
.map-grid { opacity: .28; background-image: linear-gradient(rgba(119,137,187,.16) 1px, transparent 1px), linear-gradient(90deg, rgba(119,137,187,.16) 1px, transparent 1px); background-size: 54px 54px; }
.orbit { border-color: rgba(118,133,191,.18); }
.node-image { border-color: rgba(122,137,255,.62); color: #eff2ff; background-image: radial-gradient(circle at 34% 28%, #9099ff 0, #4f5ab6 30%, #262d60 58%, #0d1121 78%); box-shadow: 0 0 25px rgba(100,113,239,.32), inset -10px -13px 22px rgba(0,0,0,.42); }
.career-node:nth-of-type(3n + 1) .node-image,
.skill-node:nth-of-type(3n + 1) .node-image { background-image: radial-gradient(circle at 34% 28%, #f1a7bb, #9b536f 44%, #251729); }
.career-node:nth-of-type(3n + 2) .node-image,
.skill-node:nth-of-type(3n + 2) .node-image { background-image: radial-gradient(circle at 34% 28%, #87e1c1, #397f76 44%, #10272d); }
.career-node:hover .node-image,
.career-node.active .node-image,
.skill-node:hover .node-image,
.skill-node.active .node-image { box-shadow: 0 0 34px rgba(114,126,255,.7), 0 0 0 5px rgba(107,118,246,.12); transform: scale(1.04); }
.node-label { color: #eef2ff; text-shadow: 0 2px 10px rgba(0,0,0,.9); }
.node-label small { color: #8d98ae; }
.node-progress,
.progress-track { background: rgba(141,153,186,.17); }
.node-progress i,
.progress-track i,
.progress-track span { background: linear-gradient(90deg, #6d77f4, #78c8f5); box-shadow: 0 0 10px rgba(108,120,245,.34); }
.career-detail { color: #edf2ff; background: linear-gradient(165deg, rgba(27,25,48,.94), rgba(14,18,29,.96)); }
.career-detail h2,
.career-detail h3,
.detail-progress-summary strong,
.planet-progress-item strong { color: #f1f4ff; }
.detail-description,
.detail-progress-summary span,
.planet-progress-item small { color: #8893aa; }
.detail-image,
.progress-planet { border-color: rgba(124,137,255,.45); box-shadow: 0 0 24px rgba(83,95,217,.2); }
.panel-close { color: #b8c2d6; border-color: rgba(148,163,184,.22); background: rgba(7,10,18,.62); }
.enter-button,
.center-message button,
.modal-close-button { border-color: transparent; color: #fff; background: linear-gradient(135deg, #606af0, #8075e8); box-shadow: 0 10px 24px rgba(82,88,203,.26); }
.enter-button:hover,
.center-message button:hover,
.modal-close-button:hover { border-color: transparent; color: #fff; background: linear-gradient(135deg, #7079fa, #8e84f0); }
.career-description-popover,
.learning-modal { border-color: rgba(148,163,184,.18); color: #edf2ff; background: rgba(14,18,29,.96); box-shadow: 0 28px 70px rgba(0,0,0,.46); backdrop-filter: blur(18px); }
.career-description-popover h2,
.learning-modal h2 { color: #f2f5ff; }
.career-description-popover p,
.content-empty { color: #8e99ad; }
.center-message { min-height: 420px; color: #8994aa; }

/* 真实数据驱动的任务卡 / 统计图 / 进度面板 */
.nebula-panels {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, .72fr) minmax(0, .92fr);
  gap: 14px;
  margin-bottom: 14px;
}

.nebula-panel {
  padding: 18px 20px;
  border: 1px solid rgba(148, 163, 184, .14);
  border-radius: 24px;
  background: rgba(16, 20, 31, .78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .03), 0 18px 48px rgba(0, 0, 0, .16);
  backdrop-filter: blur(12px);
}

.nebula-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.nebula-panel__head small { display: block; color: #626e88; font-size: 9px; letter-spacing: .18em; }
.nebula-panel__head h2 { margin: 6px 0 0; color: #edf2ff; font-size: 16px; }
.nebula-panel__head > span {
  padding: 4px 10px;
  border: 1px solid rgba(148, 163, 184, .14);
  border-radius: 999px;
  color: #8d98ae;
  font-size: 10px;
  white-space: nowrap;
}

.task-list { display: grid; gap: 10px; }

.task-card {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, .1);
  border-radius: 18px;
  color: #aab4c8;
  background: rgba(23, 28, 43, .72);
  text-align: left;
  transition: border-color .2s ease, color .2s ease, transform .2s ease, box-shadow .2s ease;
}

.task-card:hover {
  border-color: rgba(124, 137, 255, .5);
  color: #f4f6ff;
  background: linear-gradient(135deg, rgba(83, 91, 199, .32), rgba(49, 44, 102, .3));
  box-shadow: 0 8px 22px rgba(25, 26, 63, .28);
  transform: translateY(-1px);
}

.task-planet {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(114, 199, 255, .45);
  border-radius: 50%;
  color: #cfeeff;
  background: radial-gradient(circle at 34% 28%, #5eb6dd, #304770 50%, #181c31 76%);
  background-position: center;
  background-size: cover;
  font-size: 10px;
}

.task-copy { min-width: 0; }
.task-copy strong { display: block; overflow: hidden; color: inherit; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.task-copy small { display: block; margin: 3px 0 6px; color: #6e7890; font-size: 10px; }
.task-copy .progress-track { display: block; height: 4px; }
.task-card > em { color: #9aa5ff; font-size: 12px; font-style: normal; font-weight: 700; }
.task-empty { margin: 0; padding: 16px 0; color: #778298; font-size: 12px; }

.chapter-ring {
  position: relative;
  display: grid;
  width: 132px;
  height: 132px;
  margin: 4px auto 12px;
  place-items: center;
  border-radius: 50%;
  background: conic-gradient(#6d77f4 0 calc(var(--value, 0) * 1%), rgba(148, 163, 184, .16) calc(var(--value, 0) * 1%) 100%);
  box-shadow: 0 0 30px rgba(96, 110, 240, .22);
}

.chapter-ring::after {
  position: absolute;
  inset: 13px;
  border-radius: 50%;
  background: rgba(9, 12, 20, .92);
  content: '';
}

.chapter-ring span { position: relative; z-index: 1; color: #f2f5ff; font-size: 26px; font-weight: 800; letter-spacing: -.04em; }
.chapter-ring em { color: #818ba2; font-size: 12px; font-style: normal; }
.chapter-hint { margin: 0; color: #778298; font-size: 11px; line-height: 1.6; text-align: center; }

.career-progress { display: grid; gap: 11px; margin: 0; padding: 0; list-style: none; }
.career-progress li { display: grid; grid-template-columns: 88px minmax(0, 1fr) 38px; align-items: center; gap: 10px; }
.career-progress__name { overflow: hidden; color: #cdd5e6; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.career-progress .progress-track { height: 5px; }
.career-progress em { color: #858fa6; font-size: 11px; font-style: normal; text-align: right; }

/* 主题里的 background 简写会重置尺寸，这里恢复节点的星球图片铺满效果 */
.node-image,
.mini-nebula,
.detail-image,
.progress-planet,
.task-planet {
  background-position: center;
  background-size: cover;
}

@media (max-width: 1120px) {
  .nebula-hero { grid-template-columns: 1fr minmax(320px, .8fr); }
  .career-layout--selected { grid-template-columns: 210px minmax(0, 1fr) 300px; }
  .nebula-panels { grid-template-columns: minmax(0, 1.2fr) minmax(0, .8fr); }
  .nebula-panel--progress { grid-column: 1 / -1; }
}
@media (max-width: 860px) {
  .nebula-shell { width: min(100% - 24px, 760px); padding-top: 74px; }
  .nebula-hero { grid-template-columns: 1fr; min-height: 0; }
  .nebula-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .nebula-panels { grid-template-columns: 1fr; }
  .nebula-panel--progress { grid-column: auto; }
  .career-layout,
  .career-layout--selected,
  .learning-layout { height: auto; }
}
@media (max-width: 560px) {
  .nebula-metrics { grid-template-columns: 1fr; }
  .nebula-hero__intro { padding: 24px; }
}
</style>
