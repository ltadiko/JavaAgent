<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useCvStore } from '@/stores/cv'

const cv = useCvStore()
const fileInput = ref<HTMLInputElement>()

onMounted(() => cv.fetchList())

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) {
    await cv.upload(input.files[0])
  }
}
</script>

<template>
  <AppLayout>
    <h1 class="mb-6 text-2xl font-bold text-gray-900">My CV</h1>

    <!-- Upload -->
    <div class="rounded-lg bg-white p-6 shadow">
      <h2 class="mb-4 text-lg font-semibold">Upload CV</h2>
      <div @click="triggerUpload" @dragover.prevent @drop.prevent="handleFileChange"
        class="cursor-pointer rounded-lg border-2 border-dashed border-gray-300 p-8 text-center hover:border-blue-400">
        <p class="text-gray-500">📄 Drag & drop your CV here or click to browse</p>
        <p class="mt-1 text-xs text-gray-400">PDF, DOCX (max 10MB)</p>
        <input ref="fileInput" type="file" accept=".pdf,.docx" class="hidden" @change="handleFileChange" />
      </div>
      <div v-if="cv.uploading" class="mt-3 text-center text-sm text-blue-600">Uploading...</div>
      <div v-if="cv.error" class="mt-3 text-center text-sm text-red-600">{{ cv.error }}</div>
    </div>

    <!-- CV List -->
    <div class="mt-6 rounded-lg bg-white p-6 shadow">
      <h2 class="mb-4 text-lg font-semibold">My CVs</h2>
      <div v-if="cv.loading" class="text-sm text-gray-400">Loading...</div>
      <div v-else-if="cv.cvList.length" class="space-y-3">
        <div v-for="item in cv.cvList" :key="item.id" class="flex items-center justify-between rounded border p-3">
          <div>
            <p class="font-medium">{{ item.fileName }}</p>
            <p class="text-xs text-gray-500">{{ new Date(item.createdAt).toLocaleDateString() }}</p>
          </div>
          <span class="rounded-full px-2 py-1 text-xs" :class="{
            'bg-green-100 text-green-700': item.status === 'PARSED',
            'bg-yellow-100 text-yellow-700': item.status === 'PARSING',
            'bg-blue-100 text-blue-700': item.status === 'UPLOADED',
            'bg-red-100 text-red-700': item.status === 'FAILED',
          }">{{ item.status }}</span>
        </div>
      </div>
      <p v-else class="text-sm text-gray-400">No CVs uploaded yet.</p>
    </div>
  </AppLayout>
</template>
