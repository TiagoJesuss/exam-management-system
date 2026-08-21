<template>
  <v-alert v-if="error" type="error" density="compact" class="mt-3" closable @click:close="error = ''">
    {{ error }}
  </v-alert>

  <v-alert v-if="info" type="success" density="compact" class="mt-3" closable @click:close="info = ''">
    {{ info }}
  </v-alert>

  <div class="d-flex justify-end mt-3">
    <v-btn
      color="primary"
      :disabled="pages.length === 0 || loading || confirming"
      :loading="confirming"
      @click="confirm"
    >
      Confirmar
    </v-btn>
  </div>

  <div v-if="loading" class="d-flex justify-center align-center mt-8">
    <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
    <span class="ml-3">A carregar PDF…</span>
  </div>

  <v-row v-else class="mt-4">
    <v-col
      v-for="page in pages"
      :key="page.number"
      cols="12"
      md="6"
      lg="4"
    >
      <v-card>
        <v-img :src="page.previewUrl" max-height="360" contain class="bg-grey-lighten-4"></v-img>
        <v-card-text>
          <div class="text-subtitle-1 mb-2">Pergunta {{ page.number }}</div>
          <v-text-field
            v-model.number="page.maxScore"
            type="number"
            label="Cotação máxima"
            min="0"
            step="0.5"
            density="compact"
            hide-details
          ></v-text-field>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist'
import pdfWorker from 'pdfjs-dist/build/pdf.worker.mjs?url'
import RemoteService from '@/services/RemoteService'

GlobalWorkerOptions.workerSrc = pdfWorker

interface SegmentPage {
  number: number
  maxScore: number
  blob: Blob
  previewUrl: string
}

const props = defineProps<{ examId: number }>()
const emit = defineEmits<{ segmented: [] }>()
const router = useRouter()

const pages = ref<SegmentPage[]>([])
const loading = ref(true)
const confirming = ref(false)
const error = ref('')
const info = ref('')

function revokeUrls() {
  for (const page of pages.value) {
    URL.revokeObjectURL(page.previewUrl)
  }
}

async function loadPdf() {
  revokeUrls()
  pages.value = []
  loading.value = true
  error.value = ''
  info.value = ''
  try {
    const pdfBlob = await RemoteService.getExamFile(props.examId)
    const data = await pdfBlob.arrayBuffer()
    const pdf = await getDocument({ data }).promise

    const rendered: SegmentPage[] = []
    for (let i = 1; i <= pdf.numPages; i++) {
      const page = await pdf.getPage(i)
      const viewport = page.getViewport({ scale: 1.5 })
      const canvas = document.createElement('canvas')
      canvas.width = viewport.width
      canvas.height = viewport.height
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        throw new Error('Canvas context unavailable')
      }
      await page.render({ canvasContext: ctx, viewport }).promise

      const blob = await new Promise<Blob>((resolve, reject) => {
        canvas.toBlob(
          (b) => (b ? resolve(b) : reject(new Error('Failed to create image'))),
          'image/png'
        )
      })

      rendered.push({
        number: i,
        maxScore: 1,
        blob,
        previewUrl: URL.createObjectURL(blob)
      })
    }
    pages.value = rendered
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Não foi possível carregar o PDF.'
  } finally {
    loading.value = false
  }
}

onMounted(loadPdf)
watch(() => props.examId, loadPdf)

onBeforeUnmount(revokeUrls)

async function confirm() {
  if (pages.value.length === 0) return
  if (pages.value.some((p) => !p.maxScore || p.maxScore <= 0)) {
    error.value = 'Todas as cotações devem ser maiores que zero.'
    return
  }
  confirming.value = true
  error.value = ''
  try {
    await RemoteService.segmentExam(
      props.examId,
      pages.value.map((p) => ({ blob: p.blob, maxScore: p.maxScore }))
    )
    info.value = 'Segmentação confirmada.'
    emit('segmented')
    await router.push({ name: 'exams' })
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Não foi possível confirmar a segmentação.'
  } finally {
    confirming.value = false
  }
}
</script>
