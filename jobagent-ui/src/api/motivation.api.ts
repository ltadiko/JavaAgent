import apiClient from './client'

export interface MotivationLetter {
  id: string
  jobId: string
  jobTitle: string
  company: string
  cvId: string
  content: string
  editedContent: string | null
  status: 'DRAFT' | 'GENERATED' | 'EDITED' | 'SENT' | 'ARCHIVED'
  tone: string
  language: string
  wordCount: number
  version: number
  isEdited: boolean
  generatedAt: string
  updatedAt: string
}

export interface GenerateLetterRequest {
  jobId: string
  cvId?: string
  tone?: string
  language?: string
  additionalInstructions?: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const motivationApi = {
  async generate(data: GenerateLetterRequest): Promise<MotivationLetter> {
    const res = await apiClient.post('/motivations/generate', data)
    return res.data
  },

  async list(page = 0, size = 20): Promise<Page<MotivationLetter>> {
    const res = await apiClient.get('/motivations', { params: { page, size } })
    return res.data
  },

  async get(id: string): Promise<MotivationLetter> {
    const res = await apiClient.get(`/motivations/${id}`)
    return res.data
  },

  async update(id: string, content: string): Promise<MotivationLetter> {
    const res = await apiClient.put(`/motivations/${id}`, { content })
    return res.data
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/motivations/${id}`)
  },

  async getForJob(jobId: string): Promise<MotivationLetter[]> {
    const res = await apiClient.get(`/motivations/job/${jobId}`)
    return res.data
  },

  async getCount(): Promise<{ count: number }> {
    const res = await apiClient.get('/motivations/count')
    return res.data
  },
}
