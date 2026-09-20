<script setup>
import { useRouter } from 'vue-router'
import AppTabBar from '../AppTabBar.vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  backTo: { type: String, default: '/ai-tools' },
  backLabel: { type: String, default: '返回 AI 工具' },
  fullWidth: { type: Boolean, default: false },
})

const router = useRouter()

function goBack() {
  router.push(props.backTo)
}
</script>

<template>
  <div class="feature-page">
    <AppTabBar />
    <main class="ai-studio-page" :class="{ 'ai-studio-page--full': fullWidth }">
      <header class="ai-studio-page__head">
        <button type="button" class="ai-studio-page__back" @click="goBack">← {{ backLabel }}</button>
        <div class="ai-studio-page__intro">
          <span>AI STUDIO</span>
          <h1>{{ title }}</h1>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>
        <div class="ai-studio-page__actions">
          <slot name="actions" />
        </div>
      </header>
      <div class="ai-studio-page__body">
        <slot />
      </div>
    </main>
  </div>
</template>

<style scoped>
.ai-studio-page {
  width: min(1680px, calc(100% - 40px));
  margin: 0 auto;
  padding: 24px 0 48px;
}

.ai-studio-page--full {
  width: min(1760px, calc(100% - 32px));
}

.ai-studio-page__head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
  padding-bottom: 18px;
  border-bottom: 1.5px solid var(--hp-ink);
}

.ai-studio-page__back {
  min-height: 36px;
  padding: 0 16px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-cream);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.18s ease, color 0.18s ease;
}

.ai-studio-page__back:hover {
  color: var(--hp-cream);
  background: var(--hp-ink);
}

.ai-studio-page__intro > span {
  display: block;
  color: var(--hp-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.ai-studio-page__intro h1 {
  margin: 8px 0 6px;
  color: var(--hp-ink);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.ai-studio-page__intro p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 14px;
  line-height: 1.7;
}

.ai-studio-page__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.ai-studio-page__body {
  min-height: calc(100vh - 180px);
}

@media (max-width: 760px) {
  .ai-studio-page {
    width: min(100% - 24px, 680px);
  }

  .ai-studio-page__head {
    grid-template-columns: 1fr;
  }
}
</style>
