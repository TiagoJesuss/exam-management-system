<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Exames</h2>
    </v-col>
    <v-col cols="auto" class="d-flex ga-2">
      <v-btn
        v-if="auth.hasPermission('EXAM_LOCK') && !showDistribute && tab === 'exams'"
        :color="lockFlash ? 'success' : canLock ? 'warning' : undefined"
        :variant="lockFlash || !canLock ? 'text' : 'tonal'"
        prepend-icon="mdi-lock"
        :loading="locking"
        :disabled="!canLock || lockFlash"
        @click="lockSubmissions"
      >
        Bloquear submissões
      </v-btn>
      <v-btn
        v-if="auth.hasPermission('EXAM_DISTRIBUTE') && showDistribute && tab === 'exams'"
        :color="canDistribute ? 'secondary' : undefined"
        :variant="canDistribute ? 'tonal' : 'text'"
        prepend-icon="mdi-share-variant"
        :loading="distributing"
        :disabled="!canDistribute"
        @click="startDistribution"
      >
        Iniciar distribuição
      </v-btn>
      <v-btn
        v-if="auth.hasPermission('EXAM_CREATE') && tab === 'exams'"
        color="primary"
        prepend-icon="mdi-upload"
        :to="{ name: 'exam-upload' }"
      >
        Upload
      </v-btn>
    </v-col>
  </v-row>

  <v-alert
    v-if="statusMessage"
    :type="statusType"
    density="compact"
    class="mb-2"
    closable
    @click:close="statusMessage = ''"
  >
    {{ statusMessage }}
  </v-alert>

  <v-tabs v-model="tab" class="mt-2">
    <v-tab value="exams">Exames</v-tab>
    <v-tab value="grades">Notas</v-tab>
  </v-tabs>

  <v-tabs-window v-model="tab">
    <v-tabs-window-item value="exams">
      <v-data-table
        :headers="examHeaders"
        :items="exams"
        :loading="loading"
        item-key="id"
        class="text-left mt-2"
        no-data-text="Sem exames a apresentar."
      >
        <template v-slot:[`item.actions`]="{ item }">
          <v-btn
            v-if="auth.hasPermission('EXAM_READ') && item.state === 'UPLOADED'"
            size="small"
            variant="tonal"
            color="primary"
            :to="{ name: 'exam-segment', params: { id: item.id } }"
          >
            Segmentar
          </v-btn>
        </template>
      </v-data-table>
    </v-tabs-window-item>

    <v-tabs-window-item value="grades">
      <v-row class="mt-3" dense>
        <v-col cols="12" sm="6" md="4">
          <v-select
            v-if="canPickSchool"
            v-model="filterSchool"
            :items="schoolOptions"
            item-title="code"
            item-value="code"
            label="Escola"
            clearable
            density="compact"
            hide-details
          />
          <v-text-field
            v-else
            :model-value="auth.user?.schoolCode ?? ''"
            label="Escola"
            density="compact"
            hide-details
            disabled
          />
        </v-col>
        <v-col cols="12" sm="6" md="4">
          <v-select
            v-model="filterSubject"
            :items="subjectCodes"
            label="Disciplina"
            clearable
            density="compact"
            hide-details
          />
        </v-col>
      </v-row>

      <v-data-table
        :headers="gradeHeaders"
        :items="gradedExams"
        :loading="loading"
        item-key="id"
        class="text-left mt-2"
        no-data-text="Sem notas a apresentar."
      />
    </v-tabs-window-item>
  </v-tabs-window>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import RemoteService from '@/services/RemoteService'
import type ExamDto from '@/models/ExamDto'
import type SchoolDto from '@/models/SchoolDto'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const tab = ref('exams')
const exams = ref<ExamDto[]>([])
const schools = ref<SchoolDto[]>([])
const subjectCodes = ref<string[]>([])
const filterSchool = ref<string | null>(null)
const filterSubject = ref<string | null>(null)
const loading = ref(true)
const locking = ref(false)
const distributing = ref(false)
const lockFlash = ref(false)
const showDistribute = ref(false)
const canLock = ref(false)
const canDistribute = ref(false)
const statusMessage = ref('')
const statusType = ref<'success' | 'error' | 'warning' | 'info'>('success')

const canPickSchool = computed(() => auth.hasPermission('SCHOOL_READ'))

const schoolOptions = computed(() => schools.value)

const examHeaders = [
  { title: 'ID', key: 'id', sortable: true },
  { title: 'Aluno (ID)', key: 'studentId', sortable: true },
  { title: 'Disciplina', key: 'subjectCode', sortable: true },
  { title: 'Estado', key: 'state', sortable: true },
  { title: 'Nota final', key: 'finalScore', sortable: true },
  { title: 'Ações', key: 'actions', sortable: false },
]

const gradeHeaders = [
  { title: 'Aluno', key: 'studentName', sortable: true },
  { title: 'Escola', key: 'schoolCode', sortable: true },
  { title: 'Disciplina', key: 'subjectCode', sortable: true },
  { title: 'Nota final', key: 'finalScore', sortable: true },
]

const gradedExams = computed(() => {
  const school = canPickSchool.value
    ? filterSchool.value
    : auth.user?.schoolCode ?? null

  return exams.value.filter((e) => {
    if (e.state !== 'GRADED') return false
    if (school && e.schoolCode !== school) return false
    if (filterSubject.value && e.subjectCode !== filterSubject.value) return false
    return true
  })
})

function resolveAdminButtons(all: ExamDto[]) {
  const hasLocked = all.some((e) => e.state === 'LOCKED')
  const hasUploaded = all.some((e) => e.state === 'UPLOADED')
  const hasSegmented = all.some((e) => e.state === 'SEGMENTED')
  showDistribute.value = hasLocked
  canLock.value = hasSegmented
  canDistribute.value = hasLocked && !hasUploaded && !hasSegmented
}

async function loadExams(options?: { skipAdminButtons?: boolean }) {
  loading.value = true
  const all = await RemoteService.getExams()
  exams.value = all
  if (
    !options?.skipAdminButtons &&
    (auth.hasPermission('EXAM_LOCK') || auth.hasPermission('EXAM_DISTRIBUTE'))
  ) {
    resolveAdminButtons(all)
  }
  loading.value = false
}

onMounted(async () => {
  await loadExams()
  if (auth.hasPermission('SUBJECT_READ')) {
    const subjects = await RemoteService.getSubjects()
    subjectCodes.value = subjects.map((s) => s.code!).filter(Boolean)
  }
  if (canPickSchool.value) {
    schools.value = await RemoteService.getSchools()
  }
})

async function lockSubmissions() {
  if (!canLock.value) return
  const ok = confirm(
    'Bloquear submissões? Todos os exames SEGMENTED passam a LOCKED. Os ainda UPLOADED mantêm-se e podem ser segmentados. Novos uploads ficam impedidos.'
  )
  if (!ok) return
  locking.value = true
  statusMessage.value = ''
  try {
    const locked = await RemoteService.lockExams()
    statusType.value = 'success'
    statusMessage.value =
      locked.length === 0
        ? 'Nenhum exame SEGMENTED para bloquear.'
        : `${locked.length} exame(s) bloqueado(s).`
    if (locked.length > 0) {
      await loadExams({ skipAdminButtons: true })
      canLock.value = false
      lockFlash.value = true
      setTimeout(() => {
        lockFlash.value = false
        loadExams()
      }, 1500)
    } else {
      await loadExams()
    }
  } catch (e: unknown) {
    statusType.value = 'error'
    statusMessage.value = e instanceof Error ? e.message : 'Erro ao bloquear submissões.'
  } finally {
    locking.value = false
  }
}

async function startDistribution() {
  if (!canDistribute.value) return
  const ok = confirm(
    'Iniciar a distribuição das perguntas pelos professores? Só é possível quando todos os exames estão LOCKED.'
  )
  if (!ok) return
  distributing.value = true
  statusMessage.value = ''
  try {
    const result = await RemoteService.distributeExams()
    if (result.assignedCount === 0) {
      statusType.value = 'info'
      statusMessage.value = 'Nenhum exame LOCKED para distribuir.'
      await loadExams()
    } else {
      statusType.value = result.forcedSameSchoolCount > 0 ? 'warning' : 'success'
      statusMessage.value =
        result.forcedSameSchoolCount > 0
          ? `${result.assignedCount} pergunta(s) atribuída(s). ${result.forcedSameSchoolCount} foram atribuídas a professores da mesma escola do aluno por falta de alternativas.`
          : `${result.assignedCount} pergunta(s) atribuída(s) com sucesso.`
      await loadExams({ skipAdminButtons: true })
      showDistribute.value = false
      canLock.value = false
      canDistribute.value = false
    }
  } catch (e: unknown) {
    statusType.value = 'error'
    statusMessage.value = e instanceof Error ? e.message : 'Erro ao iniciar a distribuição.'
  } finally {
    distributing.value = false
  }
}
</script>
