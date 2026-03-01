<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { useJobsStore } from '@/stores/jobs'

const route = useRoute()
const jobs = useJobsStore()

onMounted(() => jobs.fetchJob(route.params.id as string))
</script>

<template>
  <AppLayout>
    <div v-if="jobs.loading" class="text-center text-gray-400">Loading...</div>
    <div v-else-if="jobs.currentJob" class="mx-auto max-w-3xl">
      <router-link to="/jobs" class="text-sm text-blue-600 hover:underline">← Back to Search</router-link>

      <div class="mt-4 rounded-lg bg-white p-6 shadow">
        <div class="flex items-start justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">{{ jobs.currentJob.title }}</h1>
            <p class="mt-1 text-lg text-gray-600">{{ jobs.currentJob.company }}</p>
            <p class="text-sm text-gray-500">📍 {{ jobs.currentJob.location }}</p>
          </div>
          <span v-if="jobs.currentJob.matchScore"
            class="rounded-full bg-green-100 px-3 py-1 text-sm font-medium text-green-700">
            {{ jobs.currentJob.matchScore }}% match
          </span>
        </div>

        <div v-if="jobs.currentJob.salaryRange" class="mt-4">
          <span class="text-sm font-medium text-gray-700">💰 {{ jobs.currentJob.salaryRange }}</span>
        </div>

        <div class="mt-6">
          <h2 class="mb-2 text-lg font-semibold">Description</h2>
          <p class="whitespace-pre-line text-gray-700">{{ jobs.currentJob.description }}</p>
        </div>

        <div v-if="jobs.currentJob.requirements" class="mt-4">
          <h2 class="mb-2 text-lg font-semibold">Requirements</h2>
          <p class="whitespace-pre-line text-gray-700">{{ jobs.currentJob.requirements }}</p>
        </div>

        <div class="mt-6 flex gap-3">
          <router-link :to="`/applications?jobId=${jobs.currentJob.id}`"
            class="rounded-md bg-blue-600 px-6 py-2 text-white hover:bg-blue-700">Apply Now</router-link>
          <a v-if="jobs.currentJob.sourceUrl" :href="jobs.currentJob.sourceUrl" target="_blank"
            class="rounded-md border px-6 py-2 text-gray-700 hover:bg-gray-50">View Original</a>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
