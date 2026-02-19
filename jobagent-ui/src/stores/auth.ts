import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Auth store — Sprint 0 placeholder.
 * Sprint 1 will implement full OAuth 2.1 PKCE flow.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const user = ref<{ id: string; name: string; email: string } | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setToken(token: string) {
    accessToken.value = token
    localStorage.setItem('access_token', token)
  }

  function logout() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem('access_token')
  }

  return { accessToken, user, isAuthenticated, setToken, logout }
})
