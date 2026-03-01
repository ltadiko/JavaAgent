<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { path: '/dashboard', label: 'Dashboard', icon: '📊' },
  { path: '/cv', label: 'My CV', icon: '📄' },
  { path: '/jobs', label: 'Job Search', icon: '🔍' },
  { path: '/motivation-letters', label: 'Cover Letters', icon: '✉️' },
  { path: '/applications', label: 'Applications', icon: '📋' },
]
</script>

<template>
  <aside class="flex w-64 flex-col border-r bg-white shadow-sm">
    <div class="flex h-16 items-center border-b px-6">
      <span class="text-xl font-bold text-blue-600">🤖 JobAgent</span>
    </div>
    <nav class="mt-4 flex-1 space-y-1 px-3">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors"
        :class="route.path.startsWith(item.path) ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'"
      >
        <span>{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>
