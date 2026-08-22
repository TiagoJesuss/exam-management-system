<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Listagem de Pessoas</h2>
    </v-col>
    <v-col cols="auto">
      <CreatePersonDialog v-if="auth.hasPermission('PERSON_CREATE')" @person-created="getPeople" />
      <EditPersonDialog
        v-if="auth.hasPermission('PERSON_UPDATE')"
        v-model="editDialogOpen"
        :person="personToEdit"
        @person-updated="getPeople"
      />
    </v-col>
  </v-row>


  <v-text-field
    v-model="search"
    label="Pesquisar"
    prepend-inner-icon="mdi-magnify"
    variant="outlined"
    hide-details
    single-line
  ></v-text-field>

  <v-data-table
    :headers="headers"
    :items="people"
    :search="search"
    :loading="loading"
    :custom-filter="fuzzySearch"
    item-key="id"
    class="text-left"
    no-data-text="Sem pessoas a apresentar."
  >
    <template v-slot:[`item.type`]="{ item }">
      <v-chip v-if="item.type === 'ADMINISTRATOR'" color="purple" text-color="white">
        Administrador
      </v-chip>
      <v-chip v-else-if="item.type === 'SCHOOL_STAFF'" color="blue" text-color="white">
        Funcionário
      </v-chip>
      <v-chip v-else-if="item.type === 'TEACHER'" color="red" text-color="white">
        Professor
      </v-chip>
      <v-chip v-else color="green" text-color="white">
        Aluno
      </v-chip>
    </template>
    <template v-slot:[`item.actions`]="{ item }">
      <v-icon
        v-if="auth.hasPermission('PERSON_IMPERSONATE') && item.id !== auth.user?.id"
        @click="impersonate(item)"
        class="mr-2"
        color="primary"
        title="Personificar"
      >mdi-account-switch</v-icon>
      <v-icon
        v-if="auth.hasPermission('PERSON_UPDATE') && canEditPerson(item)"
        @click="editPerson(item)"
        class="mr-2"
        title="Editar"
      >mdi-pencil</v-icon>
      <v-icon
        v-if="auth.hasPermission('PERSON_DELETE') && item.id !== auth.user?.id"
        @click="deletePerson(item)"
        title="Apagar"
      >mdi-delete</v-icon>
    </template>

    <template v-slot:[`item.schoolCode`]="{ item }">
      <v-chip
        v-if="item.type === 'TEACHER' && !item.schoolCode"
        size="small"
        color="warning"
      >Sem escola</v-chip>
      <span v-else>{{ item.schoolCode }}</span>
    </template>

    <template v-slot:[`item.subjectCode`]="{ item }">
      <v-chip
        v-if="item.type === 'TEACHER' && !item.subjectCode"
        size="small"
        color="warning"
      >Sem disciplina</v-chip>
      <span v-else>{{ item.subjectCode }}</span>
    </template>

  </v-data-table>

</template>

<script setup lang="ts">
import type PersonDto from '@/models/PersonDto'
import RemoteService from '@/services/RemoteService'
import CreatePersonDialog from './CreatePersonDialog.vue'
import EditPersonDialog from './EditPersonDialog.vue'
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const canEditPerson = (person: PersonDto) => {
  if (auth.user?.role !== 'SCHOOL_STAFF') return true
  return person.type === 'STUDENT' && person.schoolCode === auth.user?.schoolCode
}

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
    title: 'Tipo',
    key: 'type',
    sortable: true,
    filterable: true
  },
  {
    title: 'Ações',
    key: 'actions',
    sortable: false,
    filterable: false
  },
  {
    title: 'Escola',
    key: 'schoolCode',
    sortable: true,
    filterable: true
  },
  {
    title: 'Disciplina',
    key: 'subjectCode',
    sortable: true,
    filterable: true
  }
]

const people: PersonDto[] = reactive([])

getPeople()
async function getPeople() {
  loading.value = true
  try {
    people.splice(0, people.length)
    people.push(...(await RemoteService.getPeople()))
  } catch {
    // Erro apresentado pelo alerta global.
  } finally {
    loading.value = false
  }
}

const editPerson = (person: PersonDto) => {
  personToEdit.value = person
  editDialogOpen.value = true
}

const deletePerson = async (person: PersonDto) => {
  const ok = confirm(`Apagar ${person.name}?`)
  if (!ok) return
  try {
    await RemoteService.deletePerson(person.id!)
    await getPeople()
  } catch {
    // Erro apresentado pelo alerta global.
  }
}

// Admin-only: log in as the selected person, then refresh the list under their
// permissions.
const impersonate = async (person: any) => {
  await auth.impersonate(person.id)
  await getPeople()
}


const fuzzySearch = (value: string, search: string) => {
  // Regex to match any character in between the search characters
  let searchRegex = new RegExp(search.split('').join('.*'), 'i')
  return searchRegex.test(value)
}

const personToEdit = ref<PersonDto | null>(null)
const editDialogOpen = ref(false)

</script>
