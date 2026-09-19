<script setup>
import { onMounted } from 'vue'
import AppTabBar from '../../components/AppTabBar.vue'
import { getToken, getUserInfo } from '../../utils/auth'

onMounted(() => {
  const token = getToken()
  if (token) {
    localStorage.setItem('session_token', token)
  }

  const user = getUserInfo()
  if (user?.id) {
    localStorage.setItem('user_id', String(user.id))
  }
  if (user?.nickname || user?.realName || user?.username) {
    localStorage.setItem(
      'nickname',
      String(user.nickname || user.realName || user.username),
    )
  }

  const role = String(user?.role || user?.userRole || user?.userType || '').toLowerCase()
  const isManager =
    role === 'admin' ||
    role === 'manager' ||
    user?.isManager === true ||
    user?.is_manager === 1 ||
    user?.is_manager === '1'
  localStorage.setItem('is_manager', isManager ? '1' : '0')
})
</script>

<template>
  <div class="interview-shell">
    <AppTabBar />
    <div class="interview-shell__content">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.interview-shell {
  min-height: 100vh;
  background: #f3efe6;
}

.interview-shell__content {
  padding-top: 60px;
  min-height: 100vh;
  background: #f3efe6;
}

:deep(.app-site-header) {
  border-bottom-color: #0d0d0c;
  background: #171715;
  box-shadow: none;
}

:deep(.app-site-header__brand span) { color: #d6c28a; }

:deep(.app-site-header__nav a.router-link-active),
:deep(.app-site-header__nav a.app-site-header__nav-link--active) {
  color: #1d1c19;
  background: #f2e8d7;
  box-shadow: none;
}

@media (max-width: 760px) {
  :deep(.app-site-header__inner) { width: calc(100% - 16px); gap: 8px; }
  :deep(.app-site-header__brand) { display: none; }
  :deep(.app-site-header__nav) { justify-content: flex-start; }
  :deep(.app-site-header__nav a) { min-height: 34px; padding: 0 10px; font-size: 12px; }
}
</style>
