<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">As minhas provas</h2>
      <p class="text-medium-emphasis ml-1 mb-0">
        Provas disponibilizadas para consulta.
      </p>
    </v-col>
  </v-row>

  <v-data-table
    :headers="headers"
    :items="exams"
    :loading="loading"
    item-key="id"
    class="text-left mt-2"
    no-data-text="Sem provas disponíveis para consulta."
    hover
    @click:row="onRowClick"
  >
    <template v-slot:[`item.reviewDeadline`]="{ item }">
      {{ formatDeadline(item.reviewDeadline) }}
    </template>
  </v-data-table>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RemoteService from '@/services/RemoteService'
import type ExamDto from '@/models/ExamDto'

const router = useRouter()
const exams = ref<ExamDto[]>([])
const loading = ref(true)

const headers = [
  { title: 'Disciplina', key: 'subjectCode', sortable: true },
  { title: 'Nota final', key: 'finalScore', sortable: true },
  { title: 'Prazo de revisão', key: 'reviewDeadline', sortable: true },
]

function formatDeadline(value?: string | null) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('pt-PT')
}

function onRowClick(_e: unknown, row: { item: ExamDto }) {
  openExam(row.item)
}

function openExam(item: ExamDto) {
  if (item.id == null) return
  router.push({ name: 'my-exam-detail', params: { id: item.id } })
}

onMounted(async () => {
  try {
    exams.value = await RemoteService.getMyExams()
  } catch {
    // Erro apresentado pelo alerta global.
  } finally {
    loading.value = false
  }
})
</script>
