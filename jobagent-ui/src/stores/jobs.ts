import { defineStore } from 'pinia'
import { ref } from 'vue'
import { jobsApi, type JobListing, type JobSearchParams } from '@/api/jobs.api'

export const useJobsStore = defineStore('jobs', () => {
  const jobs = ref<JobListing[]>([])
  const currentJob = ref<JobListing | null>(null)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function search(params: JobSearchParams = {}) {
    loading.value = true
    error.value = null
    try {
      const page = await jobsApi.search(params)
      jobs.value = page.content
      totalElements.value = page.totalElements
      totalPages.value = page.totalPages
      currentPage.value = page.number
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Search failed'
    } finally {
      loading.value = false
    }
  }

  async function fetchJob(id: string) {
    loading.value = true
    try {
      currentJob.value = await jobsApi.get(id)
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to load job'
    } finally {
      loading.value = false
    }
  }

  async function fetchMatches(page = 0) {
    loading.value = true
    try {
      const result = await jobsApi.getMatches(page)
      jobs.value = result.content
      totalElements.value = result.totalElements
      totalPages.value = result.totalPages
      currentPage.value = result.number
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to load matches'
    } finally {
      loading.value = false
    }
  }

  return { jobs, currentJob, totalElements, totalPages, currentPage, loading, error, search, fetchJob, fetchMatches }
})
