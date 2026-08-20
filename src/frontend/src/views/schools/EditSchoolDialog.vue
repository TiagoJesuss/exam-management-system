<template>
  <v-dialog v-model="dialog" max-width="400">
    <v-card prepend-icon="mdi-school" title="Editar Escola">
      <v-card-text>
        <v-text-field label="Nome*" required v-model="editedSchool.name"></v-text-field>
        <v-text-field label="Código*" required v-model="editedSchool.code"></v-text-field>
        <v-text-field label="Região*" required v-model="editedSchool.region"></v-text-field>
      </v-card-text>

      <v-divider></v-divider>

      <v-card-actions>
        <v-spacer></v-spacer>

        <v-btn text="Close" variant="plain" @click="dialog = false"></v-btn>

        <v-btn
          color="primary"
          text="Save"
          :variant="isFormValid ? 'flat' : 'tonal'"
          :disabled="!isFormValid"
          @click="saveSchool"
        ></v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type SchoolDto from '@/models/SchoolDto'
import RemoteService from '@/services/RemoteService'

const emit = defineEmits(['school-updated'])

const editedSchool = ref<SchoolDto>({
  name: '',
  code: '',
  region: ''
})

const props = defineProps<{
  school: SchoolDto | null
}>()

const dialog = defineModel<boolean>({ default: false })

watch(
  () => props.school,
  (school) => {
    if (!school) return
    editedSchool.value = {
      id: school.id,
      name: school.name,
      code: school.code,
      region: school.region
    }
    dialog.value = true
  }
)

const saveSchool = async () => {
  await RemoteService.updateSchool(editedSchool.value.id!, editedSchool.value)
  emit('school-updated')
  dialog.value = false
}

const isFormValid = computed(() => {
  const s = editedSchool.value
  return !!(s.name?.trim() && s.code?.trim() && s.region?.trim())
})
</script>
