<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { clearAuth, getUserInfo } from '../utils/auth'

defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
})

const router = useRouter()
const route = useRoute()
const showProfilePanel = ref(false)
const userInfo = computed(() => getUserInfo() || {})
const avatarUrl = computed(() => userInfo.value.avatar || '')
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '未登录')
const studentId = computed(() => userInfo.value.studentId || userInfo.value.personalNumber || '—')
const avatarText = computed(() => {
  return displayName.value.slice(0, 1).toUpperCase()
})

function toggleProfilePanel() {
  showProfilePanel.value = !showProfilePanel.value
}

// 子页面（如星图探索内的 Python 学习）也需要保持所属模块的选中态
function inSection(basePath) {
  return route.path === basePath || route.path.startsWith(`${basePath}/`)
}

function openProfileRoute(path) {
  showProfilePanel.value = false
  router.push(path)
}

function handleLogout() {
  showProfilePanel.value = false
  clearAuth()
  router.replace('/login')
}

const shortcutItems = [
  { label: '我的消息', to: '/mine/messages' },
  { label: '我的课表', to: '/mine/schedule' },
  { label: '我的活动', to: '/mine/activities' },
  { label: 'AI 会话历史', to: '/mine/ai-history' },
  { label: '我的试卷', to: '/mine/papers' },
  { label: '账户设置', to: '/mine/account-settings' },
]
</script>

<template>
  <header class="app-site-header" :class="{ 'app-site-header--embedded': embedded }">
    <div class="app-site-header__inner">
      <RouterLink class="app-site-header__brand" to="/home">
        数智<span>诊断</span>港
      </RouterLink>

      <nav class="app-site-header__nav" aria-label="主导航">
        <RouterLink to="/home">首页</RouterLink>
        <RouterLink to="/map">校园地图</RouterLink>
        <RouterLink to="/activities">校园活动</RouterLink>
        <RouterLink to="/ai-tools">AI 工具</RouterLink>
        <RouterLink
          to="/career/nebula"
          :class="{ 'app-site-header__nav-link--active': inSection('/career/nebula') }"
        >星图探索</RouterLink>
        <RouterLink to="/interview">AI 面试</RouterLink>
      </nav>

      <div class="app-tab-nav__profile">
        <button class="app-tab-nav__avatar" type="button" aria-label="个人头像" @click="toggleProfilePanel">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" />
          <span v-else>{{ avatarText }}</span>
        </button>
        <transition name="profile-panel">
          <div v-if="showProfilePanel" class="app-tab-nav__panel">
            <section class="app-tab-nav__panel-section">
              <p class="app-tab-nav__name">{{ displayName }}</p>
              <p class="app-tab-nav__student">学号 {{ studentId }}</p>
            </section>

            <section class="app-tab-nav__panel-section app-tab-nav__panel-section--list">
              <button
                v-for="item in shortcutItems"
                :key="item.to"
                class="app-tab-nav__panel-row"
                type="button"
                @click="openProfileRoute(item.to)"
              >
                <span>{{ item.label }}</span>
                <span class="app-tab-nav__panel-arrow">›</span>
              </button>
            </section>

            <section class="app-tab-nav__panel-section">
              <button class="app-tab-nav__logout" type="button" @click="handleLogout">
                退出登录
              </button>
            </section>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-site-header {
  position: fixed;
  inset: 0 0 auto;
  z-index: 1000;
  height: 60px;
  border-bottom: 1px solid rgba(251, 248, 242, 0.12);
  background: #14171d;
  color: #f5f0e7;
}

.app-site-header--embedded {
  position: static;
}

.app-site-header__inner {
  display: flex;
  align-items: center;
  width: min(1440px, calc(100% - 48px));
  height: 100%;
  margin: 0 auto;
  gap: 22px;
}

.app-site-header__brand {
  flex: 0 0 auto;
  color: #f5f0e7;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-decoration: none;
  white-space: nowrap;
}

.app-site-header__brand span {
  color: #ead574;
}

.app-site-header__nav {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: 0;
  flex: 1;
  gap: 4px;
  overflow-x: auto;
  padding-right: 6px;
  scrollbar-width: none;
}

.app-site-header__nav::-webkit-scrollbar {
  display: none;
}

.app-site-header__nav a {
  display: grid;
  place-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  color: rgba(245, 240, 231, 0.72);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  white-space: nowrap;
  flex: 0 0 auto;
  transition: background 0.18s ease, color 0.18s ease;
}

.app-site-header__nav a:hover {
  color: #f5f0e7;
  background: rgba(245, 240, 231, 0.08);
}

.app-site-header__nav a.router-link-active,
.app-site-header__nav a.app-site-header__nav-link--active {
  color: #14171d;
  background: #ead574;
  font-weight: 600;
}

.app-tab-nav__avatar {
  width: 36px;
  height: 36px;
  min-height: 36px;
  border: 1px solid rgba(245, 240, 231, 0.32);
  background: #f5f0e7;
  color: #14171d;
  font-size: 14px;
  font-weight: 700;
}

.app-tab-nav__panel {
  border: 1px solid #222222;
  border-radius: 18px;
  background: #fbf8f2;
  box-shadow: none;
}

@media (max-width: 680px) {
  .app-site-header__inner {
    width: min(100%, calc(100% - 24px));
    gap: 10px;
  }

  .app-site-header__brand {
    font-size: 16px;
  }
}
</style>
