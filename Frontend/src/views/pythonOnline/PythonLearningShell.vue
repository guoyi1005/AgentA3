<script setup>
import AppTabBar from '../../components/AppTabBar.vue'
import PythonLearningNav from '../../components/pythonOnline/PythonLearningNav.vue'
</script>

<template>
  <div class="python-learning-shell">
    <AppTabBar />
    <div class="python-learning-shell__nav-row">
      <PythonLearningNav />
    </div>
    <RouterView />
  </div>
</template>

<style scoped>
.python-learning-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 86% 8%, rgba(104, 82, 220, .16), transparent 30%),
    radial-gradient(circle at 8% 32%, rgba(39, 124, 184, .12), transparent 26%),
    linear-gradient(145deg, #060810 0%, #0a0d16 48%, #070911 100%);
}

.python-learning-shell__nav-row {
  display: flex;
  justify-content: flex-end;
  width: min(1480px, calc(100% - 48px));
  min-height: 48px;
  margin: 0 auto;
  padding-top: 74px;
}

.python-learning-shell :deep(.py-bank-page),
.python-learning-shell :deep(.py-dark) {
  min-height: calc(100vh - 122px);
  padding-top: 10px;
  background: transparent;
}

@media (max-width: 720px) {
  .python-learning-shell__nav-row {
    width: min(100% - 24px, 720px);
    overflow-x: auto;
  }
}
</style>
