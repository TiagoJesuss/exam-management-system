<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Listagem de Disciplinas</h2>
    </v-col>
    <v-col cols="auto">
      <CreateSubjectDialog @subject-created="getSubjects" />
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
      <v-icon @click="editSubject(item)" class="mr-2">mdi-pencil</v-icon>
      <v-icon @click="deleteSubject(item)">mdi-delete</v-icon>
    </template>

  </v-data-table>

</template>

<script setup lang="ts">
import type SubjectDto from '@/models/SubjectDto'
import RemoteService from '@/services/RemoteService'
import CreateSubjectDialog from './CreateSubjectDialog.vue'
import { reactive, ref } from 'vue'

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
  console.log('Editing subject:', subject)
}

const deleteSubject = (subject: SubjectDto) => {
  console.log('Deleting subject:', subject)
}


const fuzzySearch = (value: string, search: string) => {
  // Regex to match any character in between the search characters
  let searchRegex = new RegExp(search.split('').join('.*'), 'i')
  return searchRegex.test(value)
}

</script>
