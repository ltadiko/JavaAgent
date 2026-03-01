import { defineStore } from 'pinia'
import { ref } from 'vue'
import { motivationApi, type MotivationLetter, type GenerateLetterRequest } from '@/api/motivation.api'

export const useMotivationStore = defineStore('motivation', () => {
  const letters = ref<MotivationLetter[]>([])
  const currentLetter = ref<MotivationLetter | null>(null)
  const totalElements = ref(0)
  const generating = ref(false)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchLetters(page = 0) {
    loading.value = true
    try {
      const result = await motivationApi.list(page)
      letters.value = result.content
      totalElements.value = result.totalElements
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to load letters'
    } finally {
      loading.value = false
    }
  }

  async function generate(data: GenerateLetterRequest) {
    generating.value = true
    error.value = null
    try {
      const letter = await motivationApi.generate(data)
      letters.value.unshift(letter)
      currentLetter.value = letter
      return letter
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Generation failed'
      throw e
    } finally {
      generating.value = false
    }
  }

  async function updateLetter(id: string, content: string) {
    try {
      const letter = await motivationApi.update(id, content)
      const idx = letters.value.findIndex((l) => l.id === id)
      if (idx >= 0) letters.value[idx] = letter
      currentLetter.value = letter
      return letter
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Update failed'
      throw e
    }
  }

  async function deleteLetter(id: string) {
    await motivationApi.delete(id)
    letters.value = letters.value.filter((l) => l.id !== id)
  }

  return { letters, currentLetter, totalElements, generating, loading, error, fetchLetters, generate, updateLetter, deleteLetter }
})
