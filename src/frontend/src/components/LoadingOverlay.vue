<template>
    <v-overlay
      :model-value="appearanceStore.loading"
      class="align-center justify-center"
    >
      <v-progress-circular
        color="primary"
        size="64"
        indeterminate
      ></v-progress-circular>
    </v-overlay>
</template>

<script setup lang="ts">
import { useAppearanceStore } from '@/stores/appearance';
import { onMounted } from 'vue';

const appearanceStore = useAppearanceStore()

onMounted(() => (appearanceStore.loading = false));
</script>