<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const fullName = ref('')
const email = ref('')
const password = ref('')
const country = ref('DE')

async function handleRegister() {
  try {
    await auth.register(fullName.value, email.value, password.value, country.value)
  } catch {
    // error displayed via store
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-gray-100">
    <div class="w-full max-w-md rounded-lg bg-white p-8 shadow-lg">
      <h1 class="mb-6 text-center text-2xl font-bold text-gray-900">🤖 Create Account</h1>

      <div v-if="auth.error" class="mb-4 rounded bg-red-50 p-3 text-sm text-red-600">{{ auth.error }}</div>

      <form @submit.prevent="handleRegister" class="space-y-4">
        <div>
          <label for="fullName" class="block text-sm font-medium text-gray-700">Full Name</label>
          <input id="fullName" v-model="fullName" type="text" required
            class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-blue-500 focus:outline-none" />
        </div>
        <div>
          <label for="email" class="block text-sm font-medium text-gray-700">Email</label>
          <input id="email" v-model="email" type="email" required
            class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-blue-500 focus:outline-none" />
        </div>
        <div>
          <label for="password" class="block text-sm font-medium text-gray-700">Password</label>
          <input id="password" v-model="password" type="password" required minlength="8"
            class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-blue-500 focus:outline-none" />
        </div>
        <div>
          <label for="country" class="block text-sm font-medium text-gray-700">Country</label>
          <select id="country" v-model="country"
            class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-blue-500 focus:outline-none">
            <option value="DE">Germany (DE)</option>
            <option value="NL">Netherlands (NL)</option>
            <option value="US">United States (US)</option>
            <option value="GB">United Kingdom (GB)</option>
            <option value="FR">France (FR)</option>
            <option value="IN">India (IN)</option>
            <option value="JP">Japan (JP)</option>
            <option value="AU">Australia (AU)</option>
          </select>
        </div>
        <button type="submit" :disabled="auth.loading"
          class="w-full rounded-md bg-blue-600 py-2 text-white hover:bg-blue-700 disabled:opacity-50">
          {{ auth.loading ? 'Creating account...' : 'Register' }}
        </button>
      </form>

      <p class="mt-4 text-center text-sm text-gray-500">
        Already have an account? <router-link to="/login" class="text-blue-600 hover:underline">Sign In</router-link>
      </p>
    </div>
  </div>
</template>
