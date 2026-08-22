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
        :color="uploadsLocked ? undefined : 'primary'"
        :variant="uploadsLocked ? 'text' : undefined"
        prepend-icon="mdi-upload"
        :disabled="uploadsLocked"
        :to="uploadsLocked ? undefined : { name: 'exam-upload' }"
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
        <template v-slot:[`item.state`]="{ item }">
          {{ stateLabel(item.state) }}
        </template>
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
      <v-row class="mt-3" dense align="center">
        <v-col cols="12" sm="6" md="3">
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
        <v-col cols="12" sm="6" md="3">
          <v-select
            v-model="filterSubject"
            :items="subjectCodes"
            label="Disciplina"
            clearable
            density="compact"
            hide-details
          />
        </v-col>
        <v-col cols="12" sm="12" md="auto" class="d-flex ga-2 flex-wrap">
          <v-btn
            v-if="auth.hasPermission('EXAM_RELEASE')"
            :color="canPublishGrades ? 'primary' : undefined"
            :variant="canPublishGrades ? 'tonal' : 'text'"
            prepend-icon="mdi-bullhorn"
            :loading="publishingGrades"
            :disabled="!canPublishGrades"
            @click="publishGrades"
          >
            Publicar notas
          </v-btn>
          <v-btn
            v-if="auth.hasPermission('EXAM_RELEASE')"
            :color="canBulkRelease ? 'secondary' : undefined"
            :variant="canBulkRelease ? 'tonal' : 'text'"
            prepend-icon="mdi-share-all"
            :loading="bulkReleasing"
            :disabled="!canBulkRelease"
            @click="bulkRelease"
          >
            Disponibilizar provas
          </v-btn>
          <v-btn
            v-if="canCloseReviews"
            variant="tonal"
            color="warning"
            prepend-icon="mdi-timer-off"
            :loading="closingReviews"
            @click="closeReviewWindows"
          >
            Fechar prazos de revisão
          </v-btn>
        </v-col>
      </v-row>

      <v-data-table
        :headers="gradeHeaders"
        :items="gradedExams"
        :loading="loading"
        item-key="id"
        class="text-left mt-2"
        no-data-text="Sem notas a apresentar."
      >
        <template v-slot:[`item.state`]="{ item }">
          {{ stateLabel(item.state) }}
          <span
            v-if="item.consultationRequested && item.state === 'GRADES_PUBLISHED'"
            class="text-caption text-medium-emphasis ml-1"
          >
            · Pedido de prova
          </span>
        </template>
        <template v-slot:[`item.actions`]="{ item }">
          <v-btn
            v-if="auth.hasPermission('EXAM_RELEASE') && item.state === 'GRADES_PUBLISHED'"
            size="small"
            variant="tonal"
            color="primary"
            :loading="releasingId === item.id"
            @click="releaseExam(item)"
          >
            Disponibilizar prova
          </v-btn>
        </template>
      </v-data-table>
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
const closingReviews = ref(false)
const releasingId = ref<number | null>(null)
const bulkReleasing = ref(false)
const publishingGrades = ref(false)
const lockFlash = ref(false)
const showDistribute = ref(false)
const canLock = ref(false)
const canDistribute = ref(false)
const uploadsLocked = ref(false)
const statusMessage = ref('')
const statusType = ref<'success' | 'error' | 'warning' | 'info'>('success')

const canPickSchool = computed(() => auth.hasPermission('SCHOOL_READ'))

const resolvedSchoolCode = computed(() =>
  canPickSchool.value ? filterSchool.value : auth.user?.schoolCode ?? null
)

const canPublishGrades = computed(() => {
  const school = resolvedSchoolCode.value
  const subject = filterSubject.value
  if (!school || !subject) return false

  const scoped = exams.value.filter(
    (e) => e.schoolCode === school && e.subjectCode === subject
  )
  if (scoped.length === 0) return false

  const ready = new Set(['GRADED', 'GRADES_PUBLISHED', 'RELEASED'])
  const allReady = scoped.every((e) => e.state != null && ready.has(e.state))
  const hasGraded = scoped.some((e) => e.state === 'GRADED')
  return allReady && hasGraded
})

const canBulkRelease = computed(() => {
  const school = resolvedSchoolCode.value
  const subject = filterSubject.value
  if (!school || !subject) return false
  return exams.value.some(
    (e) =>
      e.state === 'GRADES_PUBLISHED' &&
      e.schoolCode === school &&
      e.subjectCode === subject
  )
})

const canCloseReviews = computed(
  () =>
    auth.isAdmin &&
    exams.value.some((e) => e.state === 'GRADES_PUBLISHED' || e.state === 'RELEASED')
)

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
  { title: 'Estado', key: 'state', sortable: true },
  { title: 'Nota final', key: 'finalScore', sortable: true },
  { title: 'Ações', key: 'actions', sortable: false },
]

const stateLabels: Record<string, string> = {
  UPLOADED: 'Carregado',
  SEGMENTED: 'Segmentado',
  LOCKED: 'Bloqueado',
  IN_GRADING: 'Em correção',
  GRADED: 'Corrigido',
  GRADES_PUBLISHED: 'Notas publicadas',
  RELEASED: 'Prova disponibilizada',
  REVIEW_CLOSED: 'Revisão em curso',
  REVIEW_GRADING: 'Em revisão',
  FINAL: 'Final'
}

function stateLabel(state?: string) {
  if (!state) return ''
  return stateLabels[state] ?? state
}

const gradedExams = computed(() => {
  const school = resolvedSchoolCode.value

  return exams.value.filter((e) => {
    if (
      e.state !== 'GRADED' &&
      e.state !== 'GRADES_PUBLISHED' &&
      e.state !== 'RELEASED' &&
      e.state !== 'REVIEW_CLOSED' &&
      e.state !== 'REVIEW_GRADING' &&
      e.state !== 'FINAL'
    ) {
      return false
    }
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
  try {
    const all = await RemoteService.getExams()
    exams.value = all
    uploadsLocked.value = all.some((e) => e.state === 'LOCKED' || e.state === 'IN_GRADING')
    if (
      !options?.skipAdminButtons &&
      (auth.hasPermission('EXAM_LOCK') || auth.hasPermission('EXAM_DISTRIBUTE'))
    ) {
      resolveAdminButtons(all)
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await loadExams()
    if (auth.hasPermission('SUBJECT_READ')) {
      const subjects = await RemoteService.getSubjects()
      subjectCodes.value = subjects.map((s) => s.code!).filter(Boolean)
    }
    if (canPickSchool.value) {
      schools.value = await RemoteService.getSchools()
    }
  } catch {
    // Erro apresentado pelo alerta global.
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
    statusMessage.value = RemoteService.consumeError(e)
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
    statusMessage.value = RemoteService.consumeError(e)
  } finally {
    distributing.value = false
  }
}

async function closeReviewWindows() {
  const ok = confirm(
    'Forçar o fecho dos prazos de revisão (como se as 48 h já tivessem passado)? Exames sem pedidos passam a Final; com pedidos são redistribuídos para revisão.'
  )
  if (!ok) return
  closingReviews.value = true
  statusMessage.value = ''
  try {
    const result = await RemoteService.closeReviewWindows()
    statusType.value = 'success'
    statusMessage.value =
      result.closedToFinal === 0 && result.closedToReviewClosed === 0
        ? 'Nenhum exame com janela de revisão aberta.'
        : `Prazos fechados: ${result.closedToFinal} final(is), ${result.closedToReviewClosed} enviado(s) para revisão.`
    await loadExams()
  } catch (e: unknown) {
    statusType.value = 'error'
    statusMessage.value = RemoteService.consumeError(e)
  } finally {
    closingReviews.value = false
  }
}

async function releaseExam(item: ExamDto) {
  if (item.id == null) return
  const ok = confirm(
    `Disponibilizar a prova de ${item.studentName ?? 'aluno'} (${item.subjectCode})? O aluno poderá ver o PDF.`
  )
  if (!ok) return
  releasingId.value = item.id
  statusMessage.value = ''
  try {
    await RemoteService.releaseExam(item.id)
    statusType.value = 'success'
    statusMessage.value = 'Prova disponibilizada para consulta do PDF.'
    await loadExams()
  } catch (e: unknown) {
    statusType.value = 'error'
    statusMessage.value = RemoteService.consumeError(e)
  } finally {
    releasingId.value = null
  }
}

async function publishGrades() {
  if (!canPublishGrades.value || !resolvedSchoolCode.value || !filterSubject.value) return
  const ok = confirm(
    `Publicar todas as notas corrigidas de ${resolvedSchoolCode.value} / ${filterSubject.value}? Os alunos passam a ver as classificações e inicia-se o prazo de revisão.`
  )
  if (!ok) return
  publishingGrades.value = true
  statusMessage.value = ''
  try {
    const published = await RemoteService.publishGrades(
      canPickSchool.value ? resolvedSchoolCode.value : null,
      filterSubject.value
    )
    statusType.value = 'success'
    statusMessage.value =
      published.length === 0
        ? 'Nenhuma nota GRADED para publicar com estes filtros.'
        : `${published.length} nota(s) publicada(s). Prazo de revisão iniciado.`
    await loadExams()
  } catch (e: unknown) {
    statusType.value = 'error'
    statusMessage.value = RemoteService.consumeError(e)
  } finally {
    publishingGrades.value = false
  }
}

async function bulkRelease() {
  if (!canBulkRelease.value || !resolvedSchoolCode.value || !filterSubject.value) return
  const ok = confirm(
    `Disponibilizar todas as provas com notas publicadas de ${resolvedSchoolCode.value} / ${filterSubject.value}?`
  )
  if (!ok) return
  bulkReleasing.value = true
  statusMessage.value = ''
  try {
    const released = await RemoteService.releaseExams(
      resolvedSchoolCode.value,
      filterSubject.value
    )
    statusType.value = 'success'
    statusMessage.value =
      released.length === 0
        ? 'Nenhuma prova com notas publicadas para disponibilizar com estes filtros.'
        : `${released.length} prova(s) disponibilizada(s).`
    await loadExams()
  } catch (e: unknown) {
    statusType.value = 'error'
    statusMessage.value = RemoteService.consumeError(e)
  } finally {
    bulkReleasing.value = false
  }
}
</script>
