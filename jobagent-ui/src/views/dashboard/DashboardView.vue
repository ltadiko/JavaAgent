<script setup lang="ts">
// Sprint 6: Full dashboard with stats, charts, and application overview
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <h1 class="mb-8 text-3xl font-bold text-gray-900">Dashboard</h1>

    <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
      <div class="rounded-lg bg-white p-6 shadow">
        <p class="text-sm font-medium text-gray-500">Total Applications</p>
        <p class="mt-2 text-3xl font-semibold text-gray-900">—</p>
      </div>
      <div class="rounded-lg bg-white p-6 shadow">
        <p class="text-sm font-medium text-gray-500">Interviews</p>
        <p class="mt-2 text-3xl font-semibold text-green-600">—</p>
      </div>
      <div class="rounded-lg bg-white p-6 shadow">
        <p class="text-sm font-medium text-gray-500">Pending</p>
        <p class="mt-2 text-3xl font-semibold text-yellow-600">—</p>
      </div>
      <div class="rounded-lg bg-white p-6 shadow">
        <p class="text-sm font-medium text-gray-500">Offers</p>
        <p class="mt-2 text-3xl font-semibold text-blue-600">—</p>
      </div>
    </div>

    <p class="mt-8 text-center text-gray-400">
      Dashboard data will be populated as use cases are implemented.
    </p>
  </div>
</template>
