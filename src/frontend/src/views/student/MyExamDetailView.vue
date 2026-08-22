<template>
  <v-row align="center">
    <v-col>
      <v-btn variant="text" prepend-icon="mdi-arrow-left" class="ml-n2" :to="{ name: 'my-exams' }">
        Voltar
      </v-btn>
      <h2 class="text-left ml-1">Consulta da prova</h2>
      <p v-if="consultation" class="text-medium-emphasis ml-1 mb-0">
        {{ consultation.subjectCode }}
        — nota final: {{ consultation.finalScore ?? '—' }}
        <span v-if="consultation.reviewDeadline">
          · prazo de revisão até {{ formatDeadline(consultation.reviewDeadline) }}
        </span>
      </p>
    </v-col>
    <v-col cols="auto" v-if="consultation?.state === 'GRADES_PUBLISHED'">
      <v-btn
        color="primary"
        variant="tonal"
        prepend-icon="mdi-file-eye-outline"
        :loading="requestingConsultation"
        :disabled="consultation.consultationRequested"
        @click="requestConsultation"
      >
        {{ consultation.consultationRequested ? 'Prova pedida' : 'Pedir prova' }}
      </v-btn>
    </v-col>
  </v-row>

  <v-alert v-if="error" type="error" density="compact" class="mt-2" closable @click:close="error = ''">
    {{ error }}
  </v-alert>
  <v-alert
    v-if="statusMessage"
    type="success"
    density="compact"
    class="mt-2"
    closable
    @click:close="statusMessage = ''"
  >
    {{ statusMessage }}
  </v-alert>

  <v-row v-if="loading" class="mt-4" justify="center">
    <v-progress-circular indeterminate />
  </v-row>

  <v-row v-else-if="consultation" class="mt-2">
    <v-col cols="12" md="7">
      <iframe
        v-if="pdfUrl"
        :src="pdfUrl"
        title="PDF do exame"
        class="exam-pdf"
      />
      <v-alert v-else type="info" density="compact">
        O PDF da prova ainda não foi disponibilizado pela escola. Pode consultar as classificações à direita.
      </v-alert>
    </v-col>
    <v-col cols="12" md="5">
      <h3 class="text-h6 mb-2">Classificações por fragmento</h3>
      <v-table density="compact">
        <thead>
          <tr>
            <th class="text-left">Pergunta</th>
            <th class="text-left">Cotação máx.</th>
            <th class="text-left">Nota</th>
            <th v-if="canRequestReview" class="text-left">Revisão</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in consultation.fragments ?? []" :key="f.questionNumber">
            <td>{{ f.questionNumber }}</td>
            <td>{{ f.maxScore }}</td>
            <td>{{ f.grade ?? '—' }}</td>
            <td v-if="canRequestReview">
              <span v-if="hasReview(f.questionNumber)" class="text-caption text-medium-emphasis">
                Pedido enviado
              </span>
              <v-btn
                v-else
                size="small"
                variant="text"
                color="primary"
                @click="openReviewDialog(f.questionNumber!)"
              >
                Pedir revisão
              </v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>
      <p class="mt-4 text-body-1">
        <strong>Nota final:</strong> {{ consultation.finalScore ?? '—' }}
      </p>
    </v-col>
  </v-row>

  <v-dialog v-model="reviewDialog" max-width="480">
    <v-card>
      <v-card-title>Pedir revisão — pergunta {{ reviewQuestionNumber }}</v-card-title>
      <v-card-text>
        <v-textarea
          v-model="reviewJustification"
          label="Justificação"
          rows="4"
          auto-grow
          hide-details="auto"
        />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="reviewDialog = false">Cancelar</v-btn>
        <v-btn
          color="primary"
          variant="tonal"
          :loading="submittingReview"
          :disabled="!reviewJustification.trim()"
          @click="submitReview"
        >
          Submeter
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import RemoteService from '@/services/RemoteService'
import type ExamConsultationDto from '@/models/ExamConsultationDto'
import type ReviewRequestDto from '@/models/ReviewRequestDto'

const route = useRoute()
const consultation = ref<ExamConsultationDto | null>(null)
const reviewRequests = ref<ReviewRequestDto[]>([])
const pdfUrl = ref('')
const loading = ref(true)
const error = ref('')
const statusMessage = ref('')
const requestingConsultation = ref(false)
const reviewDialog = ref(false)
const reviewQuestionNumber = ref<number | null>(null)
const reviewJustification = ref('')
const submittingReview = ref(false)

const examId = computed(() => Number(route.params.id))

const canRequestReview = computed(() => {
  const c = consultation.value
  if (!c || c.state !== 'RELEASED' || !c.reviewDeadline) return false
  return new Date(c.reviewDeadline).getTime() > Date.now()
})

function formatDeadline(value?: string | null) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('pt-PT')
}

function hasReview(questionNumber?: number) {
  if (questionNumber == null) return false
  return reviewRequests.value.some((r) => r.questionNumber === questionNumber)
}

function openReviewDialog(questionNumber: number) {
  reviewQuestionNumber.value = questionNumber
  reviewJustification.value = ''
  reviewDialog.value = true
}

async function requestConsultation() {
  requestingConsultation.value = true
  error.value = ''
  try {
    const updated = await RemoteService.requestConsultation(examId.value)
    if (consultation.value) {
      consultation.value.consultationRequested = updated.consultationRequested
    }
    statusMessage.value = 'Pedido de prova enviado à escola.'
  } catch (e: unknown) {
    error.value = RemoteService.consumeError(e)
  } finally {
    requestingConsultation.value = false
  }
}

async function submitReview() {
  if (reviewQuestionNumber.value == null || !reviewJustification.value.trim()) return
  submittingReview.value = true
  error.value = ''
  try {
    const created = await RemoteService.submitReviewRequest(
      examId.value,
      reviewQuestionNumber.value,
      reviewJustification.value.trim()
    )
    reviewRequests.value = [...reviewRequests.value, created]
    reviewDialog.value = false
    statusMessage.value = `Pedido de revisão da pergunta ${reviewQuestionNumber.value} submetido.`
  } catch (e: unknown) {
    error.value = RemoteService.consumeError(e)
  } finally {
    submittingReview.value = false
  }
}

onMounted(async () => {
  try {
    consultation.value = await RemoteService.getExamConsultation(examId.value)
    const withPdf =
      consultation.value.state === 'RELEASED' ||
      consultation.value.state === 'REVIEW_CLOSED' ||
      consultation.value.state === 'REVIEW_GRADING' ||
      consultation.value.state === 'FINAL'
    if (withPdf) {
      try {
        const blob = await RemoteService.getExamFile(examId.value)
        pdfUrl.value = URL.createObjectURL(blob)
      } catch {
        // FINAL após notas publicadas sem release: sem PDF
      }
    }
    if (consultation.value.state === 'RELEASED') {
      reviewRequests.value = await RemoteService.getReviewRequests(examId.value)
    }
  } catch (e: unknown) {
    error.value = RemoteService.consumeError(e)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (pdfUrl.value) {
    URL.revokeObjectURL(pdfUrl.value)
  }
})
</script>

<style scoped>
.exam-pdf {
  width: 100%;
  height: 80vh;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #fafafa;
}
</style>
