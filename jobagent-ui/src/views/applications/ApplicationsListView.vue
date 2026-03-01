<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import StatsCard from '@/components/StatsCard.vue'
import { useApplicationsStore } from '@/stores/applications'

const store = useApplicationsStore()
const statusFilter = ref('')

onMounted(async () => {
  await Promise.all([store.fetchApplications(), store.fetchStats()])
})

const statusColors: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  PENDING: 'bg-yellow-100 text-yellow-700',
  PROCESSING: 'bg-yellow-100 text-yellow-700',
  SENT: 'bg-blue-100 text-blue-700',
  VIEWED: 'bg-indigo-100 text-indigo-700',
  INTERVIEW: 'bg-green-100 text-green-700',
  OFFERED: 'bg-emerald-100 text-emerald-700',
  ACCEPTED: 'bg-green-200 text-green-800',
  REJECTED: 'bg-red-100 text-red-700',
  WITHDRAWN: 'bg-gray-200 text-gray-600',
  FAILED: 'bg-red-200 text-red-800',
}
</script>

<template>
  <AppLayout>
    <h1 class="mb-6 text-2xl font-bold text-gray-900">My Applications</h1>

    <!-- Stats -->
    <div v-if="store.stats" class="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6">
      <StatsCard title="Total" :value="store.stats.total" />
      <StatsCard title="Sent" :value="store.stats.sent" color="text-blue-600" />
      <StatsCard title="Interviews" :value="store.stats.interviews" color="text-green-600" />
      <StatsCard title="Offers" :value="store.stats.offers" color="text-emerald-600" />
      <StatsCard title="Pending" :value="store.stats.pending" color="text-yellow-600" />
      <StatsCard title="Rejected" :value="store.stats.rejected" color="text-red-600" />
    </div>

    <!-- Applications List -->
    <div class="mt-6">
      <div v-if="store.loading" class="text-center text-gray-400">Loading...</div>
      <div v-else-if="store.applications.length" class="space-y-3">
        <div v-for="app in store.applications" :key="app.id"
          class="rounded-lg bg-white p-4 shadow">
          <div class="flex items-start justify-between">
            <div>
              <h3 class="font-semibold text-gray-900">{{ app.jobTitle }}</h3>
              <p class="text-sm text-gray-600">{{ app.company }} • {{ app.location }}</p>
              <p class="text-xs text-gray-400 mt-1">Applied: {{ app.submittedAt ? new Date(app.submittedAt).toLocaleDateString() : 'Draft' }}</p>
              <p v-if="app.confirmationRef" class="text-xs text-gray-400">Ref: {{ app.confirmationRef }}</p>
            </div>
            <div class="flex flex-col items-end gap-2">
              <span class="rounded-full px-2 py-1 text-xs" :class="statusColors[app.status] || 'bg-gray-100 text-gray-600'">
                {{ app.status }}
              </span>
              <div class="flex gap-2">
                <button v-if="app.status === 'DRAFT'" @click="store.submitApplication(app.id)"
                  class="text-xs text-blue-600 hover:underline">Submit</button>
                <button v-if="!['ACCEPTED', 'REJECTED', 'WITHDRAWN'].includes(app.status)"
                  @click="store.withdrawApplication(app.id)"
                  class="text-xs text-red-500 hover:underline">Withdraw</button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="rounded-lg bg-white p-8 text-center shadow">
        <p class="text-gray-400">No applications yet.</p>
        <router-link to="/jobs" class="mt-2 inline-block text-blue-600 hover:underline">Search for jobs →</router-link>
      </div>
    </div>
  </AppLayout>
</template>
