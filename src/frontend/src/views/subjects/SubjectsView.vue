<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Listagem de Disciplinas</h2>
    </v-col>
    <v-col cols="auto">
      <CreateSubjectDialog @subject-created="getSubjects" />
      <EditSubjectDialog
        v-model="editDialogOpen"
        :subject="subjectToEdit"
        @subject-updated="getSubjects"
      />
    </v-col>
  </v-row>


  <v-text-field
    v-model="search"
    label="Search"
    prepend-inner-icon="mdi-magnify"
    variant="outlined"
    hide-details
    single-line
  ></v-text-field>

  <v-data-table
    :headers="headers"
    :items="subjects"
    :search="search"
    :loading="loading"
    :custom-filter="fuzzySearch"
    item-key="id"
    class="text-left"
    no-data-text="Sem disciplinas a apresentar."
  >

    <template v-slot:[`item.actions`]="{ item }">
      <v-icon
        v-if="auth.hasPermission('SUBJECT_UPDATE')"
        @click="editSubject(item)"
        class="mr-2"
        title="Editar"
      >mdi-pencil</v-icon>
      <v-icon
        v-if="auth.hasPermission('SUBJECT_DELETE')"
        @click="deleteSubject(item)"
        title="Apagar"
      >mdi-delete</v-icon>
    </template>

  </v-data-table>

</template>

<script setup lang="ts">
import type SubjectDto from '@/models/SubjectDto'
import RemoteService from '@/services/RemoteService'
import CreateSubjectDialog from './CreateSubjectDialog.vue'
import EditSubjectDialog from './EditSubjectDialog.vue'
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

let search = ref('')
let loading = ref(true)
const headers = [
  { title: 'ID', key: 'id', value: 'id', sortable: true, filterable: false },
  {
    title: 'Nome',
    key: 'name',
    sortable: true,
    filterable: true
  },
  {
    title: 'Código',
    key: 'code',
    sortable: true,
    filterable: true
  },
  {
    title: 'Ações',
    key: 'actions',
    sortable: false,
    filterable: false
  }
]

const subjects: SubjectDto[] = reactive([])

getSubjects()
async function getSubjects() {
  subjects.splice(0, subjects.length)
  subjects.push(...(await RemoteService.getSubjects()))
  loading.value = false
  console.log(subjects)
}

const editSubject = (subject: SubjectDto) => {
  subjectToEdit.value = subject
  editDialogOpen.value = true
}

const deleteSubject = async (subject: SubjectDto) => {
  const ok = confirm(`Apagar ${subject.name}?`)
  if (!ok) return
  await RemoteService.deleteSubject(subject.id!)
  await getSubjects()
}


const fuzzySearch = (value: string, search: string) => {
  // Regex to match any character in between the search characters
  let searchRegex = new RegExp(search.split('').join('.*'), 'i')
  return searchRegex.test(value)
}

const subjectToEdit = ref<SubjectDto | null>(null)
const editDialogOpen = ref(false)

</script>
