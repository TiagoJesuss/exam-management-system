<template>
  <div class="pa-4 text-center">
    <v-dialog v-model="dialog" max-width="400">
      <template v-slot:activator="{ props: activatorProps }">
        <v-btn
          class="text-none font-weight-regular"
          prepend-icon="mdi-plus"
          text="Adicionar Escola"
          v-bind="activatorProps"
          color="primary"
        ></v-btn>
      </template>

      <v-card prepend-icon="mdi-school" title="Nova Escola">
        <v-card-text>
          <v-text-field label="Nome*" required v-model="newSchool.name"></v-text-field>
          <v-text-field label="Código*" required v-model="newSchool.code"></v-text-field>
          <v-text-field label="Região*" required v-model="newSchool.region"></v-text-field>
        </v-card-text>

        <v-divider></v-divider>

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-btn text="Fechar" variant="plain" @click="dialog = false"></v-btn>

          <v-btn
            color="primary"
            text="Guardar"
            :variant="isFormValid ? 'flat' : 'tonal'"
            :disabled="!isFormValid || saving"
            :loading="saving"
            @click="saveSchool"
          ></v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type SchoolDto from '@/models/SchoolDto'
import RemoteService from '@/services/RemoteService'

const dialog = ref(false)
const saving = ref(false)

const emit = defineEmits(['school-created'])

const newSchool = ref<SchoolDto>({
  name: '',
  region: '',
  code: ''
})

const saveSchool = async () => {
  saving.value = true
  try {
    await RemoteService.createSchool(newSchool.value)
    dialog.value = false
    emit('school-created')
  } catch {
    // Erro apresentado pelo alerta global.
  } finally {
    saving.value = false
  }
}

const isFormValid = computed(() => {
  const s = newSchool.value
  return !!(s.name?.trim() && s.code?.trim() && s.region?.trim())
})

const resetForm = () => {
  newSchool.value = {
    name: '',
    region: '',
    code: ''
  }
}

watch(dialog, (isOpen) => {
  if (!isOpen) {
    resetForm()
  }
})
</script>
