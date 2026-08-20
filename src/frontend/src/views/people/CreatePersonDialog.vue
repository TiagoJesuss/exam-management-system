<template>
  <div class="pa-4 text-center">
    <v-dialog v-model="dialog" max-width="400">
      <template v-slot:activator="{ props: activatorProps }">
        <v-btn
          class="text-none font-weight-regular"
          prepend-icon="mdi-plus"
          text="Adicionar Pessoa"
          v-bind="activatorProps"
          color="primary"
        ></v-btn>
      </template>

      <v-card prepend-icon="mdi-account" title="Nova">
        <v-card-text>
          <v-text-field label="Nome*" required v-model="newPerson.name"></v-text-field>
          <v-text-field label="Email*" type="email" required v-model="newPerson.email"></v-text-field>
          <v-text-field
            label="Palavra-passe*"
            type="password"
            required
            v-model="newPerson.password"
          ></v-text-field>

            <v-select
            :items="['Administrador', 'Funcionário', 'Professor', 'Aluno']"
            label="Categoria*"
            required
            v-model="newPerson.type"
            @update:model-value="onTypeChange"
            ></v-select>

            <v-select
            v-if="needsSubject"
            :items="subjectOptions"
            item-title="title"
            item-value="value"
            label="Disciplina"
            v-model="newPerson.subjectCode"
            ></v-select>

            <v-select
            v-if="needsSchool"
            :items="schoolOptions"
            item-title="title"
            item-value="value"
            :label="schoolLabel"
            v-model="newPerson.schoolCode"
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type PersonDto from '@/models/PersonDto'
import RemoteService from '@/services/RemoteService'
import type SchoolDto from '@/models/SchoolDto'
import type SubjectDto from '@/models/SubjectDto'

const dialog = ref(false)

const emit = defineEmits(['person-created'])

const typeMappings = {
  'Administrador': 'ADMINISTRATOR',
  'Funcionário': 'SCHOOL_STAFF',
  'Professor': 'TEACHER',
  'Aluno': 'STUDENT'
}

const newPerson = ref<PersonDto>({
  name: '',
  email: '',
  password: '',
  type: '',
  schoolCode: '',
  subjectCode: ''
})

const savePerson = async () => {
  const type = typeMappings[newPerson.value.type as keyof typeof typeMappings]

  const payload: PersonDto = {
    name: newPerson.value.name,
    email: newPerson.value.email,
    password: newPerson.value.password,
    type: type,
    schoolCode: type === 'ADMINISTRATOR' || !newPerson.value.schoolCode?.trim()
      ? undefined
      : newPerson.value.schoolCode,
    subjectCode: type !== 'TEACHER' || !newPerson.value.subjectCode?.trim()
      ? undefined
      : newPerson.value.subjectCode
  }

  await RemoteService.createPerson(payload)
  dialog.value = false
  emit('person-created')
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
    newPerson.value.schoolCode = ''
  } 
  if (type !== 'Professor') {
    newPerson.value.subjectCode = ''
  }
}

const needsSchool = computed(() =>
  newPerson.value.type !== '' && newPerson.value.type !== 'Administrador'
)

const needsSubject = computed(() =>
  newPerson.value.type === 'Professor'
)

const schoolLabel = computed(() =>
  newPerson.value.type === 'Professor' ? 'Escola' : 'Escola*'
)

const isFormValid = computed(() => {
  const p = newPerson.value
  if (!p.name?.trim()) return false
  if (!p.email?.trim()) return false
  if (!p.type) return false
  if (!p.password?.trim()) return false

  // Professor: escola e disciplina opcionais
  if (p.type === 'Professor') return true

  // Escola obrigatória só para Aluno e Funcionário
  if ((p.type === 'Aluno' || p.type === 'Funcionário') && !p.schoolCode) return false

  return true
})

const resetForm = () => {
  newPerson.value = {
    name: '',
    email: '',
    password: '',
    type: '',
    schoolCode: '',
    subjectCode: ''
  }
}

watch(dialog, (isOpen) => {
  if (!isOpen) {
    resetForm()
  }
})

</script>
