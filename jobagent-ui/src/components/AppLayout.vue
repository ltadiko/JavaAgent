<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="flex h-screen bg-gray-100">
    <AppSidebar />
    <div class="flex flex-1 flex-col overflow-hidden">
      <!-- Header -->
      <header class="flex h-16 items-center justify-between border-b bg-white px-6 shadow-sm">
        <h2 class="text-lg font-semibold text-gray-700">JobAgent</h2>
        <div class="flex items-center gap-4">
          <span v-if="auth.user" class="text-sm text-gray-600">{{ auth.user.fullName }}</span>
          <button @click="auth.logout()" class="rounded bg-red-500 px-3 py-1 text-sm text-white hover:bg-red-600">
            Logout
          </button>
        </div>
      </header>
      <!-- Main content -->
      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>
  </div>
</template>
