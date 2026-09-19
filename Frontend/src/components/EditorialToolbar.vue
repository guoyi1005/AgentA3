<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getUnreadCount } from '../api/messageCenter'
import { getUserInfo } from '../utils/auth'

defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  placeholder: {
    type: String,
    default: '搜索内容',
  },
  eyebrow: {
    type: String,
    default: 'WORKSPACE',
  },
  tone: {
    type: String,
    default: 'light',
  },
})

const emit = defineEmits(['update:modelValue'])
const router = useRouter()
const user = computed(() => getUserInfo() || {})
const displayName = computed(() => user.value.realName || user.value.nickname || user.value.username || '同学')
const avatarUrl = computed(() => user.value.avatar || user.value.avatarUrl || '')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const unreadCount = ref(0)
const unreadBadge = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))

onMounted(async () => {
  try {
    const data = await getUnreadCount()
    unreadCount.value = Number(data?.total ?? data?.count ?? data ?? 0) || 0
  } catch {
    unreadCount.value = 0
  }
})
</script>

<template>
  <div class="editorial-toolbar" :class="`editorial-toolbar--${tone}`">
    <div class="editorial-toolbar__context">
      <span class="editorial-toolbar__dot" aria-hidden="true"></span>
      <span>{{ eyebrow }}</span>
    </div>

    <label class="editorial-toolbar__search">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="11" cy="11" r="7"></circle>
        <path d="m20 20-4-4"></path>
      </svg>
      <input
        :value="modelValue"
        type="search"
        :placeholder="placeholder"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <kbd>⌘ K</kbd>
    </label>

    <div class="editorial-toolbar__actions">
      <button type="button" aria-label="查看通知" title="通知" @click="router.push('/mine/messages')">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"></path>
          <path d="M10 21h4"></path>
        </svg>
        <span v-if="unreadCount > 0" class="editorial-toolbar__notice">{{ unreadBadge }}</span>
      </button>
      <button type="button" aria-label="打开设置" title="设置" @click="router.push('/mine/account-settings')">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="3"></circle>
          <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V21h-4v-.08A1.7 1.7 0 0 0 8.94 19.4a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15 1.7 1.7 0 0 0 3.08 14H3v-4h.08A1.7 1.7 0 0 0 4.6 8.94a1.7 1.7 0 0 0-.34-1.88L4.2 7l2.83-2.83.06.06A1.7 1.7 0 0 0 8.97 4.6 1.7 1.7 0 0 0 10 3.08V3h4v.08a1.7 1.7 0 0 0 1.03 1.56 1.7 1.7 0 0 0 1.88-.34l.06-.06L19.8 7l-.06.06a1.7 1.7 0 0 0-.34 1.88A1.7 1.7 0 0 0 20.92 10H21v4h-.08A1.7 1.7 0 0 0 19.4 15Z"></path>
        </svg>
      </button>
      <button class="editorial-toolbar__profile" type="button" @click="router.push('/mine')">
        <span class="editorial-toolbar__avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" />
          <span v-else>{{ avatarText }}</span>
        </span>
        <span class="editorial-toolbar__name">{{ displayName }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.editorial-toolbar {
  display: flex;
  align-items: center;
  min-height: 54px;
  gap: 18px;
  padding: 8px 10px 8px 18px;
  border: 1px solid #24231f;
  border-radius: 20px;
  background: rgba(255, 253, 247, 0.92);
  color: #23221f;
}

.editorial-toolbar__context {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #77736b;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.16em;
  white-space: nowrap;
}

.editorial-toolbar__dot {
  width: 8px;
  height: 8px;
  border: 1px solid #22211e;
  border-radius: 50%;
  background: #b8c4a2;
}

.editorial-toolbar__search {
  display: flex;
  align-items: center;
  width: min(460px, 48vw);
  min-width: 220px;
  margin: 0 auto;
  gap: 10px;
  padding: 0 13px;
  border: 1px solid #d5d0c4;
  border-radius: 999px;
  background: #f5f1e8;
  transition: border-color .2s ease, background .2s ease;
}

.editorial-toolbar__search:focus-within {
  border-color: #22211e;
  background: #fffdf7;
}

.editorial-toolbar__search svg,
.editorial-toolbar__actions svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.editorial-toolbar__search input {
  width: 100%;
  height: 38px;
  border: 0;
  outline: 0;
  color: #25231f;
  background: transparent;
  font: inherit;
  font-size: 13px;
}

.editorial-toolbar__search input::placeholder { color: #969087; }
.editorial-toolbar__search input::-webkit-search-cancel-button { display: none; }

.editorial-toolbar__search kbd {
  color: #77736b;
  font-family: inherit;
  font-size: 10px;
  white-space: nowrap;
}

.editorial-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 7px;
}

.editorial-toolbar__actions > button {
  position: relative;
  display: grid;
  width: 38px;
  height: 38px;
  padding: 0;
  place-items: center;
  border: 1px solid #d5d0c4;
  border-radius: 50%;
  color: #2b2925;
  background: #fffdf7;
  transition: transform .18s ease, border-color .18s ease, background .18s ease;
}

.editorial-toolbar__actions > button:hover {
  border-color: #22211e;
  background: #eee7da;
  transform: translateY(-1px);
}

.editorial-toolbar__actions .editorial-toolbar__profile {
  display: flex;
  width: auto;
  min-width: 42px;
  padding: 3px 10px 3px 3px;
  gap: 8px;
  border-radius: 999px;
}

.editorial-toolbar__avatar {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border: 1px solid #22211e;
  border-radius: 50%;
  background: #d8c7dd;
  font-size: 12px;
  font-weight: 800;
}

.editorial-toolbar__avatar img { width: 100%; height: 100%; object-fit: cover; }
.editorial-toolbar__name { max-width: 92px; overflow: hidden; font-size: 12px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.editorial-toolbar__notice {
  position: absolute;
  top: 1px;
  right: 1px;
  display: grid;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  place-items: center;
  border: 1px solid #fffdf7;
  border-radius: 999px;
  color: #fff;
  background: #d88d82;
  font-size: 9px;
  font-weight: 800;
  line-height: 1;
}

.editorial-toolbar--dark {
  border-color: rgba(148, 163, 184, .18);
  color: #e8edf8;
  background: rgba(16, 20, 31, .82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .035);
  backdrop-filter: blur(16px);
}
.editorial-toolbar--dark .editorial-toolbar__context { color: #818ba3; }
.editorial-toolbar--dark .editorial-toolbar__dot { border-color: #7886ff; background: #7886ff; box-shadow: 0 0 12px rgba(120, 134, 255, .72); }
.editorial-toolbar--dark .editorial-toolbar__search { border-color: rgba(148, 163, 184, .16); color: #aab4c8; background: rgba(5, 8, 15, .64); }
.editorial-toolbar--dark .editorial-toolbar__search:focus-within { border-color: rgba(124, 137, 255, .72); background: rgba(7, 10, 18, .9); }
.editorial-toolbar--dark .editorial-toolbar__search input { color: #f1f4fb; }
.editorial-toolbar--dark .editorial-toolbar__search input::placeholder,
.editorial-toolbar--dark .editorial-toolbar__search kbd { color: #677188; }
.editorial-toolbar--dark .editorial-toolbar__actions > button { border-color: rgba(148, 163, 184, .18); color: #c9d2e5; background: rgba(7, 10, 18, .72); }
.editorial-toolbar--dark .editorial-toolbar__actions > button:hover { border-color: rgba(124, 137, 255, .68); background: rgba(52, 59, 94, .72); }
.editorial-toolbar--dark .editorial-toolbar__avatar { border-color: #7c89ff; color: #f4f6ff; background: #555fc5; }
.editorial-toolbar--dark .editorial-toolbar__notice { border-color: #10141f; background: #f29eb5; }

@media (max-width: 720px) {
  .editorial-toolbar { gap: 8px; padding-left: 10px; }
  .editorial-toolbar__context,
  .editorial-toolbar__name,
  .editorial-toolbar__search kbd { display: none; }
  .editorial-toolbar__search { width: 100%; min-width: 0; }
  .editorial-toolbar__actions .editorial-toolbar__profile { padding-right: 3px; }
}
</style>
