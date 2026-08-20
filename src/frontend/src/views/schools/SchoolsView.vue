<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Listagem de Escolas</h2>
    </v-col>
    <v-col cols="auto">
      <CreateSchoolDialog @school-created="getSchools" />
      <EditSchoolDialog
        v-model="editDialogOpen"
        :school="schoolToEdit"
        @school-updated="getSchools"
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
    :items="schools"
    :search="search"
    :loading="loading"
    :custom-filter="fuzzySearch"
    item-key="id"
    class="text-left"
    no-data-text="Sem escolas a apresentar."
  >
    <template v-slot:[`item.actions`]="{ item }">
      <v-icon
        v-if="auth.hasPermission('SCHOOL_UPDATE')"
        @click="editSchool(item)"
        class="mr-2"
        title="Editar"
      >mdi-pencil</v-icon>
      <v-icon
        v-if="auth.hasPermission('SCHOOL_DELETE')"
        @click="deleteSchool(item)"
        title="Apagar"
      >mdi-delete</v-icon>
    </template>

  </v-data-table>

</template>

<script setup lang="ts">
import type SchoolDto from '@/models/SchoolDto'
import RemoteService from '@/services/RemoteService'
import CreateSchoolDialog from './CreateSchoolDialog.vue'
import EditSchoolDialog from './EditSchoolDialog.vue'
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const schools = reactive([])

let search = ref('')
let loading = ref(true)
const headers = [
  { title: 'ID', key: 'id', sortable: true, filterable: false },
  { title: 'Nome', key: 'name', sortable: true, filterable: true },
  { title: 'Região', key: 'region', sortable: true, filterable: true },
  { title: 'Código', key: 'code', sortable: true, filterable: true },
  { title: 'Ações', key: 'actions', sortable: false, filterable: false },
]

getSchools()
async function getSchools() {
  schools.splice(0, schools.length)
  schools.push(...(await RemoteService.getSchools()))
  loading.value = false
  console.log(schools)
}

const editSchool = (school: SchoolDto) => {
  schoolToEdit.value = school
  editDialogOpen.value = true
}

const deleteSchool = async (school: SchoolDto) => {
  const ok = confirm(`Apagar ${school.name}?`)
  if (!ok) return
  await RemoteService.deleteSchool(school.id!)
  await getSchools()
}


const fuzzySearch = (value: string, search: string) => {
  // Regex to match any character in between the search characters
  let searchRegex = new RegExp(search.split('').join('.*'), 'i')
  return searchRegex.test(value)
}

const schoolToEdit = ref<SchoolDto | null>(null)
const editDialogOpen = ref(false)

</script>
