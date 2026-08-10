<template>
  <v-app-bar>
    <v-spacer></v-spacer>
    <v-btn v-for="item in navbarItems" :key="item.name" :to="item.path">
      {{ item.name }}
      <v-icon color="contrast" :icon="item.icon"></v-icon>
    </v-btn>
  </v-app-bar>
</template>

<script setup lang="ts">
defineProps<{ navbarItems: { name: string; path: string; icon: string }[] }>()
</script>
