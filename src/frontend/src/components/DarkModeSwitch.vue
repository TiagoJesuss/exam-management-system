<template>
  <v-switch @change="toggleTheme()" class="mt-6" v-model="appearanceStore.isDarkTheme"
  ></v-switch>
  <v-icon v-if="appearanceStore.isDarkTheme" icon="mdi-moon-waning-crescent"></v-icon>
  <v-icon v-else color="#ffa825" icon="mdi-white-balance-sunny"></v-icon>
</template>

<script setup lang="ts">
import { useTheme } from 'vuetify'
import { useAppearanceStore } from '@/stores/appearance'

const theme = useTheme()
const appearanceStore = useAppearanceStore()

theme.global.name.value = appearanceStore.currentTheme

const toggleTheme = () => {
  appearanceStore.toggleTheme()
  theme.global.name.value = appearanceStore.currentTheme
}
</script>
