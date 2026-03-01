import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type UserInfo } from '@/api/auth.api'
import router from '@/router'

/**
 * Auth store — Sprint 0 placeholder.
 * Sprint 1 will implement full OAuth 2.1 PKCE flow.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const user = ref<UserInfo | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setToken(token: string) {
    accessToken.value = token
    localStorage.setItem('access_token', token)
  }

  async function login(email: string, password: string) {
    loading.value = true
    error.value = null
    try {
      const res = await authApi.login({ email, password })
      setToken(res.access_token)
      await fetchUser()
      await router.push('/dashboard')
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Login failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function register(fullName: string, email: string, password: string, region?: string) {
    loading.value = true
    error.value = null
    try {
      await authApi.register({ fullName, email, password, region })
      await login(email, password)
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Registration failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchUser() {
    try {
      user.value = await authApi.me()
    } catch {
      user.value = null
    }
  }

  function logout() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem('access_token')
    router.push('/login')
  }

  return { accessToken, user, isAuthenticated, loading, error, setToken, login, register, fetchUser, logout }
})
