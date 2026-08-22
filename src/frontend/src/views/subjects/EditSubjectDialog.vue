<template>
  <v-dialog v-model="dialog" max-width="400">
    <v-card prepend-icon="mdi-subject" title="Editar Disciplina">
      <v-card-text>
        <v-text-field label="Nome*" required v-model="editedSubject.name"></v-text-field>
        <v-text-field label="Código*" required v-model="editedSubject.code"></v-text-field>
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
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type SubjectDto from '@/models/SubjectDto'
import RemoteService from '@/services/RemoteService'

const emit = defineEmits(['subject-updated'])
const saving = ref(false)

const editedSubject = ref<SubjectDto>({
  name: '',
  code: ''
})

const props = defineProps<{
  subject: SubjectDto | null
}>()

const dialog = defineModel<boolean>({ default: false })

watch(
  () => props.subject,
  (subject) => {
    if (!subject) return
    editedSubject.value = {
      id: subject.id,
      name: subject.name,
      code: subject.code
    }
    dialog.value = true
  }
)

const saveSubject = async () => {
  saving.value = true
  try {
    await RemoteService.updateSubject(editedSubject.value.id!, editedSubject.value)
    emit('subject-updated')
    dialog.value = false
  } catch {
    // Erro apresentado pelo alerta global.
  } finally {
    saving.value = false
  }
}

const isFormValid = computed(() => {
  const s = editedSubject.value
  return !!(s.name?.trim() && s.code?.trim())
})
</script>
