<script setup lang="ts">
defineProps<{
  visible: boolean
  title: string
  message: string
  primaryText?: string
  secondaryText?: string
  busy?: boolean
  dismissible?: boolean
}>()

defineEmits<{
  primary: []
  secondary: []
  close: []
}>()
</script>

<template>
  <Teleport to="body">
    <Transition name="interview-dialog-fade">
      <div
        v-if="visible"
        class="interview-dialog-overlay"
        role="presentation"
        @click.self="dismissible !== false && $emit('close')"
      >
        <section
          class="interview-dialog"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="'interview-dialog-title'"
        >
          <div v-if="busy" class="interview-dialog-spinner" aria-hidden="true"></div>
          <div v-else class="interview-dialog-mark" aria-hidden="true">!</div>
          <h2 id="interview-dialog-title">{{ title }}</h2>
          <p>{{ message }}</p>
          <div v-if="primaryText || secondaryText" class="interview-dialog-actions">
            <button
              v-if="secondaryText"
              type="button"
              class="interview-dialog-button secondary"
              @click="$emit('secondary')"
            >
              {{ secondaryText }}
            </button>
            <button
              v-if="primaryText"
              type="button"
              class="interview-dialog-button primary"
              @click="$emit('primary')"
            >
              {{ primaryText }}
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.interview-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(4, 8, 16, 0.72);
  backdrop-filter: blur(8px);
}

.interview-dialog {
  width: min(420px, 100%);
  padding: 28px;
  color: #e8eefc;
  text-align: center;
  background: linear-gradient(145deg, rgba(24, 32, 48, 0.98), rgba(13, 19, 32, 0.98));
  border: 1px solid rgba(109, 125, 255, 0.28);
  border-radius: 16px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.45), 0 0 28px rgba(92, 107, 255, 0.1);
}

.interview-dialog-mark,
.interview-dialog-spinner {
  width: 42px;
  height: 42px;
  margin: 0 auto 18px;
}

.interview-dialog-mark {
  display: grid;
  place-items: center;
  color: #b9c7ff;
  font-size: 22px;
  font-weight: 700;
  background: rgba(91, 111, 255, 0.13);
  border: 1px solid rgba(118, 137, 255, 0.35);
  border-radius: 12px;
}

.interview-dialog-spinner {
  border: 3px solid rgba(139, 154, 255, 0.18);
  border-top-color: #8ea1ff;
  border-radius: 50%;
  animation: interview-dialog-spin 0.8s linear infinite;
}

.interview-dialog h2 {
  margin: 0;
  color: #f2f5ff;
  font-size: 20px;
  font-weight: 650;
}

.interview-dialog p {
  margin: 12px 0 0;
  color: #9ea9be;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-line;
}

.interview-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}

.interview-dialog-button {
  min-width: 104px;
  height: 40px;
  padding: 0 18px;
  color: #c9d3e8;
  font: inherit;
  font-size: 14px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(151, 166, 196, 0.2);
  border-radius: 9px;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.interview-dialog-button:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(151, 166, 196, 0.34);
}

.interview-dialog-button.primary {
  color: #f5f7ff;
  background: linear-gradient(135deg, #5968dc, #6f5fd1);
  border-color: rgba(144, 156, 255, 0.54);
}

.interview-dialog-button.primary:hover {
  background: linear-gradient(135deg, #6575e8, #7b6cdd);
}

.interview-dialog-button:active {
  transform: translateY(1px);
}

.interview-dialog-button:focus-visible {
  outline: 2px solid rgba(128, 154, 255, 0.75);
  outline-offset: 2px;
}

.interview-dialog-fade-enter-active,
.interview-dialog-fade-leave-active {
  transition: opacity 0.18s ease;
}

.interview-dialog-fade-enter-from,
.interview-dialog-fade-leave-to {
  opacity: 0;
}

@keyframes interview-dialog-spin {
  to { transform: rotate(360deg); }
}
</style>
