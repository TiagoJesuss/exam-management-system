<template>
  <v-row align="center">
    <v-col>
      <v-btn variant="text" prepend-icon="mdi-arrow-left" class="ml-n2" :to="{ name: 'grading' }">
        Voltar
      </v-btn>
      <h2 class="text-left ml-1">Corrigir fragmento</h2>
      <p v-if="evaluate" class="text-medium-emphasis ml-1 mb-0">
        {{ evaluate.subjectCode }} — pergunta {{ evaluate.questionNumber }}
        (máx. {{ evaluate.maxScore }})
      </p>
    </v-col>
  </v-row>

  <v-alert v-if="error" type="error" density="compact" class="mt-2" closable @click:close="error = ''">
    {{ error }}
  </v-alert>

  <v-row v-if="loading" class="mt-4" justify="center">
    <v-progress-circular indeterminate />
  </v-row>

  <template v-else-if="evaluate">
    <v-row class="mt-2">
      <v-col cols="12" md="8">
        <img
          v-if="imageUrl"
          :src="imageUrl"
          alt="Fragmento da pergunta"
          class="question-image"
        />
        <v-alert v-else type="warning" density="compact">Imagem indisponível.</v-alert>
      </v-col>
      <v-col cols="12" md="4">
        <v-text-field
          v-model.number="grade"
          type="number"
          label="Nota"
          :hint="`Entre 0 e ${evaluate.maxScore}`"
          persistent-hint
          :min="0"
          :max="evaluate.maxScore"
          step="0.1"
          :error-messages="gradeError"
        />
        <v-btn
          color="primary"
          class="mt-3"
          block
          :loading="submitting"
          :disabled="!canSubmit"
          @click="submit"
        >
          Submeter nota
        </v-btn>
      </v-col>
    </v-row>
  </template>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RemoteService from '@/services/RemoteService'
import type EvaluateInboxDto from '@/models/EvaluateInboxDto'

const route = useRoute()
const router = useRouter()

const evaluate = ref<EvaluateInboxDto | null>(null)
const imageUrl = ref('')
const grade = ref<number | null>(null)
const loading = ref(true)
const submitting = ref(false)
const error = ref('')

const gradeError = computed(() => {
  if (grade.value == null || Number.isNaN(grade.value)) return ''
  const max = evaluate.value?.maxScore ?? 0
  if (grade.value < 0 || grade.value > max) {
    return `A nota deve estar entre 0 e ${max}.`
  }
  return ''
})

const canSubmit = computed(() => {
  return (
    evaluate.value != null &&
    grade.value != null &&
    !Number.isNaN(grade.value) &&
    !gradeError.value &&
    !submitting.value
  )
})

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    evaluate.value = await RemoteService.getEvaluate(id)
    if (evaluate.value.grade != null) {
      grade.value = evaluate.value.grade
    }
    const blob = await RemoteService.getEvaluateImage(id)
    imageUrl.value = URL.createObjectURL(blob)
  } catch (e: unknown) {
    error.value = (e as { message?: string })?.message ?? 'Não foi possível carregar a atribuição.'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
  }
})

async function submit() {
  if (!canSubmit.value || evaluate.value?.id == null || grade.value == null) return
  submitting.value = true
  error.value = ''
  try {
    await RemoteService.submitGrade(evaluate.value.id, grade.value)
    await router.push({ name: 'grading' })
  } catch (e: unknown) {
    error.value = (e as { message?: string })?.message ?? 'Não foi possível submeter a nota.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.question-image {
  width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #fafafa;
}
</style>
