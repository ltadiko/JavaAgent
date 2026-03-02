import apiClient from './client'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  country?: string
}

export interface AuthResponse {
  access_token: string
  token_type: string
  expires_in: number
}

export interface UserInfo {
  id: string
  fullName: string
  email: string
  region: string
}

export const authApi = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await apiClient.post('/auth/login', data)
    return res.data
  },

  async register(data: RegisterRequest): Promise<UserInfo> {
    const res = await apiClient.post('/auth/register', data)
    return res.data
  },

  async me(): Promise<UserInfo> {
    const res = await apiClient.get('/auth/me')
    return res.data
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout')
  },
}
