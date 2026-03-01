import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi, type DashboardSummary, type RecentActivity } from '@/api/dashboard.api'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummary | null>(null)
  const activities = ref<RecentActivity[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSummary() {
    loading.value = true
    error.value = null
    try {
      summary.value = await dashboardApi.getSummary()
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to load dashboard'
    } finally {
      loading.value = false
    }
  }

  async function fetchActivity(limit = 10) {
    try {
      activities.value = await dashboardApi.getActivity(limit)
    } catch (e: any) {
      console.error('Failed to load activity:', e)
    }
  }

  return { summary, activities, loading, error, fetchSummary, fetchActivity }
})
