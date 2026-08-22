<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Registo de Auditoria</h2>
    </v-col>
  </v-row>

  <v-alert v-if="error" type="error" class="mb-4" closable @click:close="error = ''">
    {{ error }}
  </v-alert>

  <v-text-field
    v-model="search"
    label="Pesquisar"
    prepend-inner-icon="mdi-magnify"
    variant="outlined"
    hide-details
    single-line
    class="mb-4"
  />

  <v-data-table
    :headers="headers"
    :items="logs"
    :search="search"
    :loading="loading"
    item-key="id"
    class="text-left"
    no-data-text="Sem registos de auditoria."
  >
    <template v-slot:[`item.timestamp`]="{ item }">
      {{ formatTimestamp(item.timestamp) }}
    </template>
    <template v-slot:[`item.actor`]="{ item }">
      {{ item.actorName || item.actorEmail || '—' }}
    </template>
    <template v-slot:[`item.action`]="{ item }">
      <v-chip size="small" variant="tonal">{{ actionLabel(item.action) }}</v-chip>
    </template>
  </v-data-table>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import RemoteService from '@/services/RemoteService'
import type AuditLogDto from '@/models/AuditLogDto'

const logs = ref<AuditLogDto[]>([])
const loading = ref(false)
const search = ref('')
const error = ref('')

const headers = [
  { title: 'Data', key: 'timestamp', sortable: true },
  { title: 'Utilizador', key: 'actor', sortable: false },
  { title: 'Acção', key: 'action', sortable: true },
  { title: 'Mensagem', key: 'message', sortable: false },
]

const ACTION_LABELS: Record<string, string> = {
  GRADE_SUBMITTED: 'Nota',
  EXAMS_LOCKED: 'Bloqueio',
  EXAMS_DISTRIBUTED: 'Distribuição',
  GRADES_PUBLISHED: 'Publicação',
  EXAM_RELEASED: 'Disponibilização',
  REVIEW_REQUESTED: 'Pedido de revisão',
  REVIEW_DISTRIBUTED: 'Distribuição de revisão',
  PERSON_CREATED: 'Pessoa criada',
  PERSON_UPDATED: 'Pessoa editada',
  PERSON_DELETED: 'Pessoa apagada',
  SCHOOL_CREATED: 'Escola criada',
  SCHOOL_UPDATED: 'Escola editada',
  SCHOOL_DELETED: 'Escola apagada',
  SUBJECT_CREATED: 'Disciplina criada',
  SUBJECT_UPDATED: 'Disciplina editada',
  SUBJECT_DELETED: 'Disciplina apagada',
  EXAM_UPLOADED: 'Upload de exame',
  EXAM_SEGMENTED: 'Segmentação',
}

function actionLabel(action?: string): string {
  if (!action) return '—'
  return ACTION_LABELS[action] ?? action
}

function formatTimestamp(value?: string): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('pt-PT')
}

async function loadLogs() {
  loading.value = true
  error.value = ''
  try {
    logs.value = await RemoteService.getAuditLogs()
  } catch (e: unknown) {
    error.value = RemoteService.consumeError(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>
