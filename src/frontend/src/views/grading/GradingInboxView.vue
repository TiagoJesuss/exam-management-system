<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Correcções</h2>
      <p class="text-medium-emphasis ml-1 mb-0">
        Atribuições pendentes ou em progresso (sem identificação do aluno).
      </p>
    </v-col>
  </v-row>

  <v-data-table
    :headers="headers"
    :items="evaluates"
    :loading="loading"
    item-key="id"
    class="text-left mt-2"
    no-data-text="Sem atribuições a apresentar."
    hover
    @click:row="(_e: unknown, { item }: { item: EvaluateInboxDto }) => openEvaluate(item)"
  >
    <template v-slot:[`item.state`]="{ item }">
      {{ stateLabel(item.state) }}
    </template>
  </v-data-table>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RemoteService from '@/services/RemoteService'
import type EvaluateInboxDto from '@/models/EvaluateInboxDto'

const router = useRouter()
const evaluates = ref<EvaluateInboxDto[]>([])
const loading = ref(true)

const headers = [
  { title: 'ID', key: 'id', sortable: true },
  { title: 'Estado', key: 'state', sortable: true },
  { title: 'Disciplina', key: 'subjectCode', sortable: true },
  { title: 'Pergunta', key: 'questionNumber', sortable: true },
  { title: 'Cotação máx.', key: 'maxScore', sortable: true },
]

const stateLabels: Record<string, string> = {
  PENDING: 'Pendente',
  IN_PROGRESS: 'Em progresso',
  GRADED: 'Corrigido'
}

function stateLabel(state?: string) {
  if (!state) return ''
  return stateLabels[state] ?? state
}

function openEvaluate(item: EvaluateInboxDto) {
  if (item.id == null) return
  router.push({ name: 'grading-detail', params: { id: item.id } })
}

onMounted(async () => {
  evaluates.value = await RemoteService.getMyEvaluates()
  loading.value = false
})
</script>
