import apiClient from './client'

export interface JobApplication {
  id: string
  jobId: string
  jobTitle: string
  company: string
  location: string
  cvId: string
  cvFileName: string
  letterId: string | null
  status: string
  applyMethod: string | null
  confirmationRef: string | null
  failureReason: string | null
  additionalMessage: string | null
  submittedAt: string | null
  sentAt: string | null
  viewedAt: string | null
  responseAt: string | null
  createdAt: string
  updatedAt: string
}

export interface SubmitApplicationRequest {
  jobId: string
  cvId: string
  letterId?: string
  additionalMessage?: string
}

export interface ApplicationStats {
  total: number
  drafts: number
  pending: number
  sent: number
  interviews: number
  offers: number
  rejected: number
  byStatus: Record<string, number>
}

export interface TimelineEntry {
  id: string
  eventType: string
  oldStatus: string | null
  newStatus: string | null
  details: string | null
  timestamp: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const applicationsApi = {
  async create(data: SubmitApplicationRequest): Promise<JobApplication> {
    const res = await apiClient.post('/applications', data)
    return res.data
  },

  async submit(id: string): Promise<JobApplication> {
    const res = await apiClient.post(`/applications/${id}/submit`)
    return res.data
  },

  async list(page = 0, size = 20): Promise<Page<JobApplication>> {
    const res = await apiClient.get('/applications', { params: { page, size } })
    return res.data
  },

  async listByStatus(status: string, page = 0, size = 20): Promise<Page<JobApplication>> {
    const res = await apiClient.get(`/applications/status/${status}`, { params: { page, size } })
    return res.data
  },

  async get(id: string): Promise<JobApplication> {
    const res = await apiClient.get(`/applications/${id}`)
    return res.data
  },

  async updateStatus(id: string, status: string, notes?: string): Promise<JobApplication> {
    const res = await apiClient.put(`/applications/${id}/status`, { status, notes })
    return res.data
  },

  async withdraw(id: string): Promise<JobApplication> {
    const res = await apiClient.post(`/applications/${id}/withdraw`)
    return res.data
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/applications/${id}`)
  },

  async getStats(): Promise<ApplicationStats> {
    const res = await apiClient.get('/applications/stats')
    return res.data
  },

  async getTimeline(id: string): Promise<TimelineEntry[]> {
    const res = await apiClient.get(`/applications/${id}/timeline`)
    return res.data
  },

  async hasApplied(jobId: string): Promise<boolean> {
    const res = await apiClient.get(`/applications/check/${jobId}`)
    return res.data.applied
  },
}
