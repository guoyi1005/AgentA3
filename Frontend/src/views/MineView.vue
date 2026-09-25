<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'
import { clearAuth, getUserInfo } from '../utils/auth'

const router = useRouter()
const userInfo = computed(() => getUserInfo() || {})
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '未登录')
const studentId = computed(() => userInfo.value.studentId || userInfo.value.personalNumber || '—')
const menuItems = [
  { label: '我的消息', to: '/mine/messages' },
  { label: '我的课表', to: '/mine/schedule' },
  { label: '我的活动', to: '/mine/activities' },
  { label: 'AI 会话历史', to: '/mine/ai-history' },
  { label: '我的试卷', to: '/mine/papers' },
  { label: '个人画像', to: '/profile-radar' },
  { label: '账户设置', to: '/mine/account-settings' },
]

function openMenuItem(item) {
  router.push(item.to)
}

function logout() {
  clearAuth()
  router.replace('/login')
}
</script>

<template>
  <div class="feature-page mine-page">
    <main class="mine-main">
      <header class="topbar"><h1>个人中心</h1></header>
      <section class="profile-card feature-card">
        <div class="avatar">{{ displayName.slice(0, 1).toUpperCase() }}</div>
        <div><h2>{{ displayName }}</h2><p class="muted">学号 {{ studentId }}</p></div>
      </section>
      <section class="menu-card feature-card">
        <button
          v-for="item in menuItems"
          :key="item.to"
          class="menu-row"
          type="button"
          @click="openMenuItem(item)"
        >
          <span>{{ item.label }}</span>
          <span class="arrow">›</span>
        </button>
      </section>
      <button class="logout-button" type="button" @click="logout">退出登录</button>
    </main>
    <AppTabBar />
  </div>
</template>

<style scoped>
.mine-page {
  min-height: 100vh;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.mine-main {
  display: grid;
  grid-template-columns: minmax(260px, 330px) minmax(0, 1fr);
  gap: 18px;
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
  padding: 32px 0 56px;
}

.topbar {
  grid-column: 1 / -1;
  display: block;
  padding: 0;
}

.topbar h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
}

.profile-card {
  align-self: start;
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 128px;
  padding: 22px;
}

.avatar {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 64px;
  height: 64px;
  border: 1px solid var(--hp-line);
  border-radius: 18px;
  color: var(--hp-ink);
  background: var(--hp-blue);
  font-size: 25px;
  font-weight: 800;
}

.profile-card h2 {
  margin: 0 0 6px;
  color: var(--hp-ink);
  font-size: 20px;
  font-weight: 700;
}

.profile-card p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.menu-card {
  overflow: hidden;
}

.menu-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 58px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(23, 23, 23, 0.12);
  color: var(--hp-ink);
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  transition: background 0.18s ease;
}

.menu-row:hover {
  background: rgba(23, 23, 23, 0.05);
}

.menu-row:last-child {
  border-bottom: 0;
}

.arrow {
  color: var(--hp-muted);
  font-size: 23px;
  line-height: 1;
}

.logout-button {
  grid-column: 1;
  width: 100%;
  min-height: 46px;
  border: 1px solid #d9b0ab;
  border-radius: 999px;
  color: #a54239;
  background: transparent;
  font-weight: 700;
  transition: color 0.18s ease, background 0.18s ease;
}

.logout-button:hover {
  color: #fffdf8;
  background: #a54239;
}

@media (max-width: 760px) {
  .mine-main {
    grid-template-columns: 1fr;
    width: calc(100% - 32px);
    padding: 24px 0 40px;
  }

  .topbar,
  .logout-button {
    grid-column: auto;
  }
}
</style>
