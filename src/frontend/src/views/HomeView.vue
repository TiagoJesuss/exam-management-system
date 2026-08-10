<template>
  <div class="text-center">
    <h1>Hello!</h1>
    <p>TODO: Home page for {{ appName }}</p>
  </div>
</template>

<script setup lang="ts">
const appName = import.meta.env.VITE_NAME;
</script>
