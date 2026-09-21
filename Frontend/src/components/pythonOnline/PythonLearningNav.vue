<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const links = [
  { to: '/career/nebula/python', label: '题库', match: (path) => path === '/career/nebula/python' || path.startsWith('/career/nebula/python/practice') },
  { to: '/career/nebula/python/plan', label: '学习规划', match: (path) => path === '/career/nebula/python/plan' || path === '/career/nebula/python/resources' },
  { to: '/career/nebula/python/knowledge-graph', label: '知识图谱', match: (path) => path === '/career/nebula/python/knowledge-graph' },
]

function isActive(link) {
  return link.match(route.path)
}
</script>

<template>
  <nav class="py-learning-nav" aria-label="Python 学习导航">
    <RouterLink
      v-for="link in links"
      :key="link.to"
      :to="link.to"
      class="py-learning-nav__link"
      :class="{ 'py-learning-nav__link--active': isActive(link) }"
    >
      {{ link.label }}
    </RouterLink>
  </nav>
</template>

<style scoped>
.py-learning-nav {
  display: flex;
  gap: 6px;
  padding: 4px;
  border: 1px solid rgba(148, 163, 184, .12);
  border-radius: 999px;
  background: rgba(5, 8, 15, .66);
}

.py-learning-nav__link {
  padding: 8px 18px;
  border-radius: 999px;
  color: #7e899f;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.py-learning-nav__link:hover {
  color: #cbd3e5;
  background: rgba(255, 255, 255, 0.045);
}

.py-learning-nav__link--active {
  color: #ffffff;
  background: linear-gradient(135deg, #626bf0, #8177ed);
  box-shadow: 0 7px 22px rgba(88, 93, 220, 0.3);
}

.py-learning-nav__link:active {
  transform: translateY(1px);
}
</style>
