import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  applicationsApi,
  type JobApplication,
  type ApplicationStats,
  type TimelineEntry,
  type SubmitApplicationRequest,
} from '@/api/applications.api'

export const useApplicationsStore = defineStore('applications', () => {
  const applications = ref<JobApplication[]>([])
  const currentApp = ref<JobApplication | null>(null)
  const stats = ref<ApplicationStats | null>(null)
  const timeline = ref<TimelineEntry[]>([])
  const totalElements = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchApplications(page = 0) {
    loading.value = true
    try {
      const result = await applicationsApi.list(page)
      applications.value = result.content
      totalElements.value = result.totalElements
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to load applications'
    } finally {
      loading.value = false
    }
  }

  async function createApplication(data: SubmitApplicationRequest) {
    loading.value = true
    error.value = null
    try {
      const app = await applicationsApi.create(data)
      applications.value.unshift(app)
      return app
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to create application'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function submitApplication(id: string) {
    try {
      const app = await applicationsApi.submit(id)
      const idx = applications.value.findIndex((a) => a.id === id)
      if (idx >= 0) applications.value[idx] = app
      return app
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Submit failed'
      throw e
    }
  }

  async function fetchStats() {
    try {
      stats.value = await applicationsApi.getStats()
    } catch (e: any) {
      console.error('Failed to load stats:', e)
    }
  }

  async function fetchTimeline(id: string) {
    try {
      timeline.value = await applicationsApi.getTimeline(id)
    } catch (e: any) {
      console.error('Failed to load timeline:', e)
    }
  }

  async function withdrawApplication(id: string) {
    const app = await applicationsApi.withdraw(id)
    const idx = applications.value.findIndex((a) => a.id === id)
    if (idx >= 0) applications.value[idx] = app
    return app
  }

  return {
    applications, currentApp, stats, timeline, totalElements, loading, error,
    fetchApplications, createApplication, submitApplication, fetchStats, fetchTimeline, withdrawApplication,
  }
})
