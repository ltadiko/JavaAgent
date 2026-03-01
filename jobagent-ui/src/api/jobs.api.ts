import apiClient from './client'

export interface JobListing {
  id: string
  title: string
  company: string
  location: string
  description: string
  requirements: string
  salaryRange: string
  jobType: string
  sourceUrl: string
  matchScore: number
  postedAt: string
  createdAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface JobSearchParams {
  query?: string
  location?: string
  jobType?: string
  page?: number
  size?: number
}

export const jobsApi = {
  async search(params: JobSearchParams = {}): Promise<Page<JobListing>> {
    const res = await apiClient.get('/jobs', { params })
    return res.data
  },

  async get(id: string): Promise<JobListing> {
    const res = await apiClient.get(`/jobs/${id}`)
    return res.data
  },

  async getMatches(page = 0, size = 20): Promise<Page<JobListing>> {
    const res = await apiClient.get('/jobs/matches', { params: { page, size } })
    return res.data
  },
}
