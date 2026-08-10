<template>
  <v-dialog v-model="show">
    <v-alert v-model="show" type="error" closable close-text="Fechar alerta">
      {{ appearanceStore.currentErrorMessage ?? '??' }}
    </v-alert>
  </v-dialog>
</template>

<script setup lang="ts">
import { useAppearanceStore } from '@/stores/appearance';
import { ref, watch, watchEffect } from 'vue';

const appearanceStore = useAppearanceStore()

const show = ref(true);
watchEffect(() => (show.value = !appearanceStore.isStackEmpty));
watch(show, (value) => {
  if (!value) {
    appearanceStore.popError();
  }
});
</script>

<style scoped lang="scss">
.v-dialog__container {
  display: unset !important;
}

.v-alert {
  z-index: 9999;
  position: absolute;
  left: 20px;
  top: 80px;
  width: calc(100% - 40px);
}
</style>
