import apiClient from './client'

export interface CvDetails {
  id: string
  fileName: string
  fileSize: number
  contentType: string
  status: 'UPLOADED' | 'PARSING' | 'PARSED' | 'FAILED'
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CvParsedData {
  fullName: string
  email: string
  phone: string
  currentTitle: string
  summary: string
  skills: string[]
  experience: { company: string; title: string; location: string; startDate: string; endDate: string; description: string }[]
  education: { institution: string; degree: string; field: string; year: string }[]
  languages: string[]
  certifications: string[]
}

export const cvApi = {
  async upload(file: File): Promise<CvDetails> {
    const form = new FormData()
    form.append('file', file)
    const res = await apiClient.post('/cv/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data
  },

  async list(): Promise<CvDetails[]> {
    const res = await apiClient.get('/cv')
    return res.data
  },

  async get(id: string): Promise<CvDetails> {
    const res = await apiClient.get(`/cv/${id}`)
    return res.data
  },

  async getParsedData(id: string): Promise<CvParsedData> {
    const res = await apiClient.get(`/cv/${id}/parsed`)
    return res.data
  },

  async parse(id: string): Promise<void> {
    await apiClient.post(`/cv/${id}/parse`)
  },
}
