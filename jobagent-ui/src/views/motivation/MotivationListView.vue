<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useMotivationStore } from '@/stores/motivation'

const store = useMotivationStore()

onMounted(() => store.fetchLetters())
</script>

<template>
  <AppLayout>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Cover Letters</h1>
    </div>

    <div v-if="store.loading" class="text-center text-gray-400">Loading...</div>
    <div v-else-if="store.letters.length" class="space-y-3">
      <div v-for="letter in store.letters" :key="letter.id"
        class="rounded-lg bg-white p-4 shadow">
        <div class="flex items-start justify-between">
          <div>
            <h3 class="font-semibold text-gray-900">{{ letter.jobTitle }}</h3>
            <p class="text-sm text-gray-600">{{ letter.company }}</p>
            <p class="text-xs text-gray-400 mt-1">{{ letter.tone }} • {{ letter.language }} • {{ letter.wordCount }} words</p>
          </div>
          <div class="flex items-center gap-2">
            <span class="rounded-full px-2 py-1 text-xs" :class="{
              'bg-blue-100 text-blue-700': letter.status === 'GENERATED',
              'bg-green-100 text-green-700': letter.status === 'EDITED',
              'bg-gray-100 text-gray-700': letter.status === 'DRAFT',
              'bg-purple-100 text-purple-700': letter.status === 'SENT',
            }">{{ letter.status }}</span>
            <button @click="store.deleteLetter(letter.id)"
              class="text-xs text-red-500 hover:text-red-700">Delete</button>
          </div>
        </div>
        <div class="mt-3 rounded bg-gray-50 p-3 text-sm text-gray-700 max-h-32 overflow-y-auto">
          {{ letter.content?.substring(0, 300) }}{{ letter.content?.length > 300 ? '...' : '' }}
        </div>
      </div>
    </div>
    <div v-else class="rounded-lg bg-white p-8 text-center shadow">
      <p class="text-gray-400">No cover letters yet.</p>
      <p class="mt-2 text-sm text-gray-400">Generate a cover letter from a job detail page.</p>
    </div>
  </AppLayout>
</template>
