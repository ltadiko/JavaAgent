import { defineStore } from 'pinia'
import { ref } from 'vue'
import { cvApi, type CvDetails, type CvParsedData } from '@/api/cv.api'

export const useCvStore = defineStore('cv', () => {
  const cvList = ref<CvDetails[]>([])
  const currentCv = ref<CvDetails | null>(null)
  const parsedData = ref<CvParsedData | null>(null)
  const uploading = ref(false)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchList() {
    loading.value = true
    try {
      cvList.value = await cvApi.list()
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to load CVs'
    } finally {
      loading.value = false
    }
  }

  async function upload(file: File) {
    uploading.value = true
    error.value = null
    try {
      const cv = await cvApi.upload(file)
      cvList.value.unshift(cv)
      currentCv.value = cv
      return cv
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Upload failed'
      throw e
    } finally {
      uploading.value = false
    }
  }

  async function fetchParsedData(id: string) {
    try {
      parsedData.value = await cvApi.getParsedData(id)
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Failed to load parsed data'
    }
  }

  async function triggerParse(id: string) {
    try {
      await cvApi.parse(id)
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Parse failed'
    }
  }

  return { cvList, currentCv, parsedData, uploading, loading, error, fetchList, upload, fetchParsedData, triggerParse }
})
