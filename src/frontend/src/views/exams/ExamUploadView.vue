<template>
  <v-row align="center">
    <v-col cols="auto" class="pr-0">
      <v-btn
        icon="mdi-arrow-left"
        variant="text"
        :to="{ name: 'exams' }"
        title="Voltar à listagem"
      ></v-btn>
    </v-col>
    <v-col>
      <h2 class="text-left">{{ examId ? `Segmentar exame #${examId}` : 'Upload de Exame' }}</h2>
    </v-col>
    <v-col v-if="examId" cols="auto">
      <v-btn variant="tonal" @click="resetUpload">Novo upload</v-btn>
    </v-col>
  </v-row>

  <template v-if="!examId">
    <v-card max-width="480" class="mt-4">
      <v-card-text>
        <v-select
          :items="studentOptions"
          item-title="title"
          item-value="value"
          label="Aluno*"
          v-model="studentId"
        ></v-select>

        <v-select
          :items="subjectOptions"
          item-title="title"
          item-value="value"
          label="Disciplina*"
          v-model="subjectCode"
        ></v-select>

        <v-file-input
          label="PDF do exame*"
          accept=".pdf,application/pdf"
          prepend-icon="mdi-file-pdf-box"
          v-model="file"
          show-size
        ></v-file-input>
      </v-card-text>

      <v-divider></v-divider>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="primary"
          text="Carregar"
          :variant="isFormValid ? 'flat' : 'tonal'"
          :disabled="!isFormValid || uploading"
          :loading="uploading"
          @click="upload"
        ></v-btn>
      </v-card-actions>
    </v-card>
  </template>

  <template v-else>
    <v-alert type="success" density="compact" class="mt-3 mb-0">
      Exame carregado com sucesso. Segmente as páginas abaixo.
    </v-alert>
    <p class="text-medium-emphasis mt-2 mb-0">
      Cada página do PDF corresponde a uma pergunta. Ajuste a cotação máxima se necessário.
    </p>
    <ExamSegmentPanel :exam-id="examId" />
  </template>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import RemoteService from '@/services/RemoteService'
import type PersonDto from '@/models/PersonDto'
import type SubjectDto from '@/models/SubjectDto'
import { useAuthStore } from '@/stores/auth'
import ExamSegmentPanel from './ExamSegmentPanel.vue'

const auth = useAuthStore()

const students = ref<PersonDto[]>([])
const subjects = ref<SubjectDto[]>([])
const studentId = ref<number | null>(null)
const subjectCode = ref('')
const file = ref<File | File[] | null>(null)
const uploading = ref(false)
const examId = ref<number | null>(null)

onMounted(async () => {
  try {
    const people = await RemoteService.getPeople()
    students.value = people.filter((p) => {
      if (p.type !== 'STUDENT') return false
      if (auth.user?.schoolCode && p.schoolCode !== auth.user.schoolCode) return false
      return true
    })
    subjects.value = await RemoteService.getSubjects()
  } catch {
    // Erro apresentado pelo alerta global.
  }
})

const studentOptions = computed(() =>
  students.value.map((s) => ({
    title: `${s.name} (${s.email})`,
    value: s.id
  }))
)

const subjectOptions = computed(() =>
  subjects.value.map((s) => ({
    title: `${s.code} - ${s.name}`,
    value: s.code
  }))
)

const selectedFile = computed(() => {
  if (!file.value) return null
  return Array.isArray(file.value) ? file.value[0] ?? null : file.value
})

const isFormValid = computed(() =>
  !!(studentId.value && subjectCode.value && selectedFile.value)
)

const upload = async () => {
  if (!studentId.value || !subjectCode.value || !selectedFile.value) return
  uploading.value = true
  try {
    const exam = await RemoteService.uploadExam(
      selectedFile.value,
      studentId.value,
      subjectCode.value
    )
    studentId.value = null
    subjectCode.value = ''
    file.value = null
    examId.value = exam.id ?? null
  } catch {
    // Erro apresentado pelo alerta global.
  } finally {
    uploading.value = false
  }
}

const resetUpload = () => {
  examId.value = null
}
</script>
