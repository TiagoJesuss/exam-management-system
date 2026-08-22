<template>
  <div class="pa-4 text-center">
    <v-dialog v-model="dialog" max-width="400">
      <template v-slot:activator="{ props: activatorProps }">
        <v-btn
          class="text-none font-weight-regular"
          prepend-icon="mdi-plus"
          text="Adicionar Disciplina"
          v-bind="activatorProps"
          color="primary"
        ></v-btn>
      </template>

      <v-card prepend-icon="mdi-subject" title="Nova Disciplina">
        <v-card-text>
          <v-text-field label="Nome*" required v-model="newSubject.name"></v-text-field>
          <v-text-field label="Código*" required v-model="newSubject.code"></v-text-field>
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
            @click="saveSubject"
          ></v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type SubjectDto from '@/models/SubjectDto'
import RemoteService from '@/services/RemoteService'

const dialog = ref(false)
const saving = ref(false)

const emit = defineEmits(['subject-created'])

const newSubject = ref<SubjectDto>({
  name: '',
  code: ''
})

const saveSubject = async () => {
  saving.value = true
  try {
    await RemoteService.createSubject(newSubject.value)
    dialog.value = false
    emit('subject-created')
  } catch {
    // Erro apresentado pelo alerta global.
  } finally {
    saving.value = false
  }
}

const isFormValid = computed(() => {
  const s = newSubject.value
  return !!(s.name?.trim() && s.code?.trim())
})

const resetForm = () => {
  newSubject.value = {
    name: '',
    code: ''
  }
}

watch(dialog, (isOpen) => {
  if (!isOpen) {
    resetForm()
  }
})
</script>
