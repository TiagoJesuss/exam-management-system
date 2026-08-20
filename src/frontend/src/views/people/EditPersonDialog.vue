<template>
  <v-dialog v-model="dialog" max-width="400">
    <v-card prepend-icon="mdi-account" title="Editar">
      <v-card-text>
        <v-text-field label="Nome*" required v-model="editedPerson.name"></v-text-field>
        <v-text-field label="Email*" type="email" required v-model="editedPerson.email"></v-text-field>
        <v-text-field
          label="Nova palavra-passe (opcional)"
          type="password"
          v-model="editedPerson.password"
        ></v-text-field>

        <v-select
          :items="['Administrador', 'Funcionário', 'Professor', 'Aluno']"
          label="Categoria*"
          required
          v-model="editedPerson.type"
          @update:model-value="onTypeChange"
        ></v-select>

        <v-select
          v-if="needsSubject"
          :items="subjectOptions"
          item-title="title"
          item-value="value"
          label="Disciplina"
          v-model="editedPerson.subjectCode"
        ></v-select>

        <v-select
          v-if="needsSchool"
          :items="schoolOptions"
          item-title="title"
          item-value="value"
          :label="schoolLabel"
          v-model="editedPerson.schoolCode"
        ></v-select>
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
          @click="savePerson"
        ></v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type PersonDto from '@/models/PersonDto'
import RemoteService from '@/services/RemoteService'
import type SchoolDto from '@/models/SchoolDto'
import type SubjectDto from '@/models/SubjectDto'

const emit = defineEmits(['person-updated'])

const typeMappings = {
  'Administrador': 'ADMINISTRATOR',
  'Funcionário': 'SCHOOL_STAFF',
  'Professor': 'TEACHER',
  'Aluno': 'STUDENT'
}

const reverseTypeMappings = {
  'ADMINISTRATOR': 'Administrador',
  'SCHOOL_STAFF': 'Funcionário',
  'TEACHER': 'Professor',
  'STUDENT': 'Aluno'
}

const editedPerson = ref<PersonDto>({
  name: '',
  email: '',
  password: '',
  type: '',
  schoolCode: '',
  subjectCode: ''
})

const props = defineProps<{
  person: PersonDto | null
}>()

const dialog = defineModel<boolean>({ default:false })

watch(
  () => props.person,
  (person) => {
    if (!person) return
    editedPerson.value = {
      id: person.id,
      name: person.name,
      email: person.email,
      password: '', // vazio = manter password actual
      type: reverseTypeMappings[person.type as keyof typeof reverseTypeMappings] ?? '',
      schoolCode: person.schoolCode ?? '',
      subjectCode: person.subjectCode ?? ''
    }
    dialog.value = true
  }
)

const savePerson = async () => {
  const type = typeMappings[editedPerson.value.type as keyof typeof typeMappings]

  const payload: PersonDto = {
    id: editedPerson.value.id,
    name: editedPerson.value.name,
    email: editedPerson.value.email,
    password: editedPerson.value.password?.trim() ? editedPerson.value.password : undefined,
    type: type,
    schoolCode: type === 'ADMINISTRATOR' || !editedPerson.value.schoolCode?.trim()
      ? undefined
      : editedPerson.value.schoolCode,
    subjectCode: type !== 'TEACHER' || !editedPerson.value.subjectCode?.trim()
      ? undefined
      : editedPerson.value.subjectCode
  }

  await RemoteService.updatePerson(editedPerson.value.id!, payload)

  emit('person-updated')
  dialog.value = false
}

const schools = ref<SchoolDto[]>([])

const subjects = ref<SubjectDto[]>([])

onMounted(async () => { 
  schools.value = await RemoteService.getSchools()
  subjects.value = await RemoteService.getSubjects() 
})

const schoolOptions = computed(() =>
  schools.value.map(s => ({
    title: `${s.code} - ${s.name}`,
    value: s.code
  }))
)

const subjectOptions = computed(() =>
  subjects.value.map(s => ({
    title: `${s.code} - ${s.name}`,
    value: s.code
  }))
)

const onTypeChange = (type: string) => {
  if (type === 'Administrador') {
    editedPerson.value.schoolCode = ''
  } 
  if (type !== 'Professor') {
    editedPerson.value.subjectCode = ''
  }
}

const needsSchool = computed(() =>
  editedPerson.value.type !== '' && editedPerson.value.type !== 'Administrador'
)

const needsSubject = computed(() =>
  editedPerson.value.type === 'Professor'
)

const schoolLabel = computed(() =>
  editedPerson.value.type === 'Professor' ? 'Escola' : 'Escola*'
)

const isFormValid = computed(() => {
  const p = editedPerson.value
  if (!p.name?.trim()) return false
  if (!p.email?.trim()) return false
  if (!p.type) return false

  // Professor: escola e disciplina opcionais
  if (p.type === 'Professor') return true

  // Escola obrigatória só para Aluno e Funcionário
  if ((p.type === 'Aluno' || p.type === 'Funcionário') && !p.schoolCode) return false

  // Password NÃO é obrigatória no edit
  return true
})

</script>
