<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useJobsStore } from '@/stores/jobs'

const jobs = useJobsStore()
const query = ref('')
const location = ref('')

onMounted(() => jobs.search())

function handleSearch() {
  jobs.search({ query: query.value, location: location.value })
}
</script>

<template>
  <AppLayout>
    <h1 class="mb-6 text-2xl font-bold text-gray-900">Job Search</h1>

    <!-- Search Bar -->
    <div class="rounded-lg bg-white p-4 shadow">
      <form @submit.prevent="handleSearch" class="flex gap-3">
        <input v-model="query" placeholder="Job title, skills, or company..."
          class="flex-1 rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none" />
        <input v-model="location" placeholder="Location..."
          class="w-48 rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none" />
        <button type="submit" class="rounded-md bg-blue-600 px-6 py-2 text-white hover:bg-blue-700">Search</button>
      </form>
    </div>

    <!-- Results -->
    <div class="mt-4">
      <p class="mb-3 text-sm text-gray-500">{{ jobs.totalElements }} jobs found</p>
      <div v-if="jobs.loading" class="text-center text-gray-400">Searching...</div>
      <div v-else-if="jobs.jobs.length" class="space-y-3">
        <router-link v-for="job in jobs.jobs" :key="job.id" :to="`/jobs/${job.id}`"
          class="block rounded-lg bg-white p-4 shadow transition hover:shadow-md">
          <div class="flex items-start justify-between">
            <div>
              <h3 class="font-semibold text-gray-900">{{ job.title }}</h3>
              <p class="text-sm text-gray-600">{{ job.company }} • {{ job.location }}</p>
              <p v-if="job.salaryRange" class="mt-1 text-xs text-gray-500">💰 {{ job.salaryRange }}</p>
            </div>
            <span v-if="job.matchScore" class="rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-700">
              {{ job.matchScore }}% match
            </span>
          </div>
        </router-link>
      </div>
      <p v-else class="text-center text-sm text-gray-400">No jobs found. Try a different search.</p>
    </div>
  </AppLayout>
</template>
