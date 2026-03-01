<script setup lang="ts">
defineProps<{
  title: string
  value: string | number
  color?: string
  subtitle?: string
}>()
</script>

<template>
  <div class="rounded-lg bg-white p-6 shadow">
    <p class="text-sm font-medium text-gray-500">{{ title }}</p>
    <p class="mt-2 text-3xl font-semibold" :class="color || 'text-gray-900'">{{ value }}</p>
    <p v-if="subtitle" class="mt-1 text-xs text-gray-400">{{ subtitle }}</p>
  </div>
</template>
