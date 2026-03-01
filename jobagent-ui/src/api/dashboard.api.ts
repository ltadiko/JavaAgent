import apiClient from './client'

export interface DashboardSummary {
  user: { name: string; email: string; memberSince: string; region: string }
  cv: { count: number; latestParsedAt: string | null; skillsCount: number; topSkills: string[] }
  jobs: { matchesCount: number; topMatchScore: number; newJobsToday: number; savedJobs: number }
  applications: { total: number; drafts: number; pending: number; sent: number; interviews: number; offers: number; rejected: number; withdrawn: number }
  letters: { count: number; latestAt: string | null }
}

export interface RecentActivity {
  id: string
  type: string
  title: string
  description: string
  entityType: string
  entityId: string
  timestamp: string
}

export const dashboardApi = {
  async getSummary(): Promise<DashboardSummary> {
    const res = await apiClient.get('/dashboard')
    return res.data
  },

  async getActivity(limit = 10): Promise<RecentActivity[]> {
    const res = await apiClient.get('/dashboard/activity', { params: { limit } })
    return res.data
  },
}
