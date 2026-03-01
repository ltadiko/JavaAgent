<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import StatsCard from '@/components/StatsCard.vue'
import { useDashboardStore } from '@/stores/dashboard'

const dashboard = useDashboardStore()

onMounted(async () => {
  await Promise.all([dashboard.fetchSummary(), dashboard.fetchActivity()])
})
</script>

<template>
  <AppLayout>
    <h1 class="mb-6 text-2xl font-bold text-gray-900">Dashboard</h1>

    <div v-if="dashboard.loading" class="text-center text-gray-500">Loading...</div>

    <template v-else-if="dashboard.summary">
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatsCard title="Applications" :value="dashboard.summary.applications.total" color="text-gray-900" />
        <StatsCard title="Interviews" :value="dashboard.summary.applications.interviews" color="text-green-600" />
        <StatsCard title="Pending" :value="dashboard.summary.applications.pending + dashboard.summary.applications.sent" color="text-yellow-600" />
        <StatsCard title="Offers" :value="dashboard.summary.applications.offers" color="text-blue-600" />
      </div>

      <!-- Second row -->
      <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatsCard title="CVs Uploaded" :value="dashboard.summary.cv.count" />
        <StatsCard title="Skills Detected" :value="dashboard.summary.cv.skillsCount" />
        <StatsCard title="Cover Letters" :value="dashboard.summary.letters.count" />
      </div>

      <!-- Top Skills -->
      <div v-if="dashboard.summary.cv.topSkills?.length" class="mt-6 rounded-lg bg-white p-6 shadow">
        <h2 class="mb-3 text-lg font-semibold text-gray-800">Top Skills</h2>
        <div class="flex flex-wrap gap-2">
          <span v-for="skill in dashboard.summary.cv.topSkills" :key="skill"
            class="rounded-full bg-blue-100 px-3 py-1 text-sm text-blue-700">{{ skill }}</span>
        </div>
      </div>

      <!-- Recent Activity -->
      <div class="mt-6 rounded-lg bg-white p-6 shadow">
        <h2 class="mb-4 text-lg font-semibold text-gray-800">Recent Activity</h2>
        <div v-if="dashboard.activities.length" class="space-y-3">
          <div v-for="a in dashboard.activities" :key="a.id" class="flex items-start gap-3 border-b pb-3 last:border-0">
            <div class="mt-0.5 h-2 w-2 rounded-full bg-blue-500" />
            <div>
              <p class="text-sm font-medium text-gray-800">{{ a.title }}</p>
              <p class="text-xs text-gray-500">{{ a.description }}</p>
              <p class="text-xs text-gray-400">{{ new Date(a.timestamp).toLocaleDateString() }}</p>
            </div>
          </div>
        </div>
        <p v-else class="text-sm text-gray-400">No recent activity yet.</p>
      </div>

      <!-- Quick Actions -->
      <div class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <router-link to="/cv" class="rounded-lg bg-blue-600 p-4 text-center text-white shadow hover:bg-blue-700">📄 Upload CV</router-link>
        <router-link to="/jobs" class="rounded-lg bg-green-600 p-4 text-center text-white shadow hover:bg-green-700">🔍 Search Jobs</router-link>
        <router-link to="/applications" class="rounded-lg bg-purple-600 p-4 text-center text-white shadow hover:bg-purple-700">📋 My Applications</router-link>
      </div>
    </template>

    <div v-else class="text-center text-gray-400">
      <p>Welcome to JobAgent! Start by uploading your CV.</p>
    </div>
  </AppLayout>
</template>
