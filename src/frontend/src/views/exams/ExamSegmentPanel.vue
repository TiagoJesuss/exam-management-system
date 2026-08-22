<template>
  <v-alert v-if="error" type="error" density="compact" class="mt-3" closable @click:close="error = ''">
    {{ error }}
  </v-alert>

  <v-alert v-if="info" type="success" density="compact" class="mt-3" closable @click:close="info = ''">
    {{ info }}
  </v-alert>

  <div class="d-flex flex-wrap align-center justify-space-between ga-3 mt-3">
    <v-btn-toggle v-model="mode" mandatory density="compact" color="primary" :disabled="loading || confirming">
      <v-btn value="pages">Páginas</v-btn>
      <v-btn value="crop">Recortar</v-btn>
    </v-btn-toggle>

    <v-btn
      color="primary"
      :disabled="!canConfirm || loading || confirming"
      :loading="confirming"
      @click="confirm"
    >
      Confirmar
    </v-btn>
  </div>

  <p v-if="!loading && mode === 'crop'" class="text-medium-emphasis text-body-2 mt-2 mb-0">
    Arraste sobre o documento para marcar uma pergunta (pode atravessar páginas). Apague e redesenhe se precisar de ajustar.
  </p>

  <div v-if="loading" class="d-flex justify-center align-center mt-8">
    <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
    <span class="ml-3">A carregar PDF…</span>
  </div>

  <template v-else-if="mode === 'pages'">
    <v-row class="mt-4">
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
              class="mb-2"
            ></v-text-field>
            <v-select
              v-model="page.questionType"
              :items="questionTypeOptions"
              item-title="title"
              item-value="value"
              label="Tipo de pergunta"
              density="compact"
              hide-details
            ></v-select>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </template>

  <template v-else>
    <v-row class="mt-4">
      <v-col cols="12" md="8">
        <div
          class="page-stage bg-grey-lighten-4"
          @mousedown="onDrawStart"
        >
          <img
            v-for="page in pages"
            :key="page.number"
            :src="page.previewUrl"
            :alt="`Página ${page.number}`"
            class="page-image"
            draggable="false"
          />
          <div
            v-for="crop in crops"
            :key="crop.id"
            class="crop-box"
            :style="boxStyle(crop)"
          >
            <span class="crop-label">{{ questionIndex(crop) }}</span>
          </div>
          <div
            v-if="draft"
            class="crop-box crop-box--draft"
            :style="draftStyle"
          ></div>
        </div>
      </v-col>

      <v-col cols="12" md="4">
        <div class="text-subtitle-1 mb-2">Perguntas ({{ crops.length }})</div>
        <v-alert v-if="crops.length === 0" type="info" density="compact" variant="tonal">
          Ainda não há recortes. Desenhe rectângulos no documento.
        </v-alert>
        <v-card
          v-for="crop in crops"
          :key="crop.id"
          class="mb-3"
          variant="outlined"
        >
          <v-card-text class="pb-2">
            <div class="d-flex align-center justify-space-between mb-2">
              <span class="text-subtitle-2">Pergunta {{ questionIndex(crop) }}</span>
              <v-btn
                icon="mdi-delete"
                size="small"
                variant="text"
                density="compact"
                title="Apagar"
                @click="removeCrop(crop.id)"
              ></v-btn>
            </div>
            <v-text-field
              v-model.number="crop.maxScore"
              type="number"
              label="Cotação máxima"
              min="0"
              step="0.5"
              density="compact"
              hide-details
              class="mb-2"
            ></v-text-field>
            <v-select
              v-model="crop.questionType"
              :items="questionTypeOptions"
              item-title="title"
              item-value="value"
              label="Tipo de pergunta"
              density="compact"
              hide-details
            ></v-select>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </template>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist'
import pdfWorker from 'pdfjs-dist/build/pdf.worker.mjs?url'
import RemoteService from '@/services/RemoteService'

GlobalWorkerOptions.workerSrc = pdfWorker

const MIN_CROP_PX = 8

type QuestionType = 'MULTIPLE_CHOICE' | 'SHORT_ANSWER' | 'LONG_ANSWER'

interface SegmentPage {
  number: number
  maxScore: number
  questionType: QuestionType
  blob: Blob
  previewUrl: string
  width: number
  height: number
}

interface CropBox {
  id: number
  x: number
  y: number
  w: number
  h: number
  maxScore: number
  questionType: QuestionType
}

interface DraftBox {
  startX: number
  startY: number
  currentX: number
  currentY: number
}

type SegmentMode = 'pages' | 'crop'

const props = defineProps<{ examId: number }>()
const emit = defineEmits<{ segmented: [] }>()
const router = useRouter()

const mode = ref<SegmentMode>('pages')
const pages = ref<SegmentPage[]>([])
const crops = ref<CropBox[]>([])
const draft = ref<DraftBox | null>(null)
const nextCropId = ref(1)
const loading = ref(true)
const confirming = ref(false)
const error = ref('')
const info = ref('')

const questionTypeOptions = [
  { title: 'Escolha múltipla', value: 'MULTIPLE_CHOICE' },
  { title: 'Resposta curta', value: 'SHORT_ANSWER' },
  { title: 'Resposta longa', value: 'LONG_ANSWER' }
]

const canConfirm = computed(() =>
  mode.value === 'pages' ? pages.value.length > 0 : crops.value.length > 0
)

const docWidth = computed(() => pages.value[0]?.width ?? 0)

const docHeight = computed(() =>
  pages.value.reduce((sum, page) => sum + page.height, 0)
)

const draftStyle = computed(() => {
  if (!draft.value) return {}
  const rect = normalizeDocRect(
    draft.value.startX,
    draft.value.startY,
    draft.value.currentX,
    draft.value.currentY
  )
  return boxStyle({ ...rect, id: 0, maxScore: 0 })
})

function revokeUrls() {
  for (const page of pages.value) {
    URL.revokeObjectURL(page.previewUrl)
  }
}

function clearCrops() {
  stopDrawListeners()
  crops.value = []
  draft.value = null
  nextCropId.value = 1
}

watch(mode, () => {
  clearCrops()
  error.value = ''
})

function questionIndex(crop: CropBox) {
  return crops.value.findIndex((c) => c.id === crop.id) + 1
}

function normalizeDocRect(x1: number, y1: number, x2: number, y2: number) {
  const width = docWidth.value
  const height = docHeight.value
  const left = Math.max(0, Math.min(x1, x2))
  const top = Math.max(0, Math.min(y1, y2))
  const right = Math.min(width, Math.max(x1, x2))
  const bottom = Math.min(height, Math.max(y1, y2))
  return {
    x: left,
    y: top,
    w: right - left,
    h: bottom - top
  }
}

let drawStageEl: HTMLElement | null = null

function stopDrawListeners() {
  window.removeEventListener('mousemove', onWindowDrawMove)
  window.removeEventListener('mouseup', onWindowDrawEnd)
  drawStageEl = null
}

function pointerToDoc(clientX: number, clientY: number, stageEl: HTMLElement) {
  const rect = stageEl.getBoundingClientRect()
  const width = docWidth.value
  const height = docHeight.value
  if (rect.width === 0 || rect.height === 0 || width === 0 || height === 0) {
    return { x: 0, y: 0 }
  }
  const x = ((clientX - rect.left) / rect.width) * width
  const y = ((clientY - rect.top) / rect.height) * height
  return {
    x: Math.min(width, Math.max(0, x)),
    y: Math.min(height, Math.max(0, y))
  }
}

function boxStyle(crop: Pick<CropBox, 'x' | 'y' | 'w' | 'h'>) {
  const width = docWidth.value || 1
  const height = docHeight.value || 1
  return {
    left: `${(crop.x / width) * 100}%`,
    top: `${(crop.y / height) * 100}%`,
    width: `${(crop.w / width) * 100}%`,
    height: `${(crop.h / height) * 100}%`
  }
}

function onDrawStart(event: MouseEvent) {
  if (event.button !== 0 || pages.value.length === 0) return
  event.preventDefault()
  stopDrawListeners()
  drawStageEl = event.currentTarget as HTMLElement
  const { x, y } = pointerToDoc(event.clientX, event.clientY, drawStageEl)
  draft.value = {
    startX: x,
    startY: y,
    currentX: x,
    currentY: y
  }
  window.addEventListener('mousemove', onWindowDrawMove)
  window.addEventListener('mouseup', onWindowDrawEnd)
}

function onWindowDrawMove(event: MouseEvent) {
  if (!draft.value || !drawStageEl) return
  const { x, y } = pointerToDoc(event.clientX, event.clientY, drawStageEl)
  draft.value = { ...draft.value, currentX: x, currentY: y }
}

function onWindowDrawEnd(event: MouseEvent) {
  if (!draft.value || !drawStageEl) {
    stopDrawListeners()
    draft.value = null
    return
  }
  const { x, y } = pointerToDoc(event.clientX, event.clientY, drawStageEl)
  const rect = normalizeDocRect(draft.value.startX, draft.value.startY, x, y)
  draft.value = null
  stopDrawListeners()
  if (rect.w < MIN_CROP_PX || rect.h < MIN_CROP_PX) return
  crops.value.push({
    id: nextCropId.value++,
    x: rect.x,
    y: rect.y,
    w: rect.w,
    h: rect.h,
    maxScore: 1,
    questionType: 'SHORT_ANSWER'
  })
}

function removeCrop(id: number) {
  crops.value = crops.value.filter((c) => c.id !== id)
}

async function loadPdf() {
  revokeUrls()
  pages.value = []
  clearCrops()
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
        questionType: 'SHORT_ANSWER',
        blob,
        previewUrl: URL.createObjectURL(blob),
        width: canvas.width,
        height: canvas.height
      })
    }
    pages.value = rendered
  } catch (e: unknown) {
    error.value = RemoteService.consumeError(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadPdf)
watch(() => props.examId, loadPdf)

onBeforeUnmount(() => {
  revokeUrls()
  clearCrops()
})

async function loadPageImage(page: SegmentPage): Promise<HTMLImageElement> {
  const img = new Image()
  img.src = page.previewUrl
  await img.decode()
  return img
}

async function cropToBlob(crop: CropBox): Promise<Blob> {
  const outWidth = Math.max(1, Math.round(crop.w))
  const pieces: { canvas: HTMLCanvasElement; height: number }[] = []
  let offsetY = 0

  for (const page of pages.value) {
    const pageTop = offsetY
    const pageBottom = offsetY + page.height
    offsetY = pageBottom

    const intersectTop = Math.max(crop.y, pageTop)
    const intersectBottom = Math.min(crop.y + crop.h, pageBottom)
    if (intersectBottom <= intersectTop) continue

    const srcY = intersectTop - pageTop
    const srcH = intersectBottom - intersectTop
    const srcX = Math.max(0, Math.min(crop.x, page.width))
    const srcRight = Math.max(srcX, Math.min(crop.x + crop.w, page.width))
    const srcW = srcRight - srcX
    if (srcW <= 0 || srcH <= 0) continue

    const img = await loadPageImage(page)
    const piece = document.createElement('canvas')
    piece.width = outWidth
    piece.height = Math.max(1, Math.round(srcH))
    const ctx = piece.getContext('2d')
    if (!ctx) {
      throw new Error('Canvas context unavailable')
    }
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, piece.width, piece.height)
    const destX = Math.round(((srcX - crop.x) / crop.w) * outWidth)
    const destW = Math.max(1, Math.round((srcW / crop.w) * outWidth))
    ctx.drawImage(img, srcX, srcY, srcW, srcH, destX, 0, destW, piece.height)
    pieces.push({ canvas: piece, height: piece.height })
  }

  if (pieces.length === 0) {
    throw new Error('Recorte sem área válida')
  }

  const totalHeight = pieces.reduce((sum, p) => sum + p.height, 0)
  const canvas = document.createElement('canvas')
  canvas.width = outWidth
  canvas.height = totalHeight
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('Canvas context unavailable')
  }
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  let dy = 0
  for (const piece of pieces) {
    ctx.drawImage(piece.canvas, 0, dy)
    dy += piece.height
  }

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (b) => (b ? resolve(b) : reject(new Error('Failed to create crop image'))),
      'image/png'
    )
  })
}

async function confirm() {
  if (!canConfirm.value) return

  if (mode.value === 'pages') {
    if (pages.value.some((p) => !p.maxScore || p.maxScore <= 0)) {
      error.value = 'Todas as cotações devem ser maiores que zero.'
      return
    }
    if (pages.value.some((p) => !p.questionType)) {
      error.value = 'Seleccione o tipo de cada pergunta.'
      return
    }
  } else if (crops.value.some((c) => !c.maxScore || c.maxScore <= 0)) {
    error.value = 'Todas as cotações devem ser maiores que zero.'
    return
  } else if (crops.value.some((c) => !c.questionType)) {
    error.value = 'Seleccione o tipo de cada pergunta.'
    return
  }

  confirming.value = true
  error.value = ''
  try {
    const items =
      mode.value === 'pages'
        ? pages.value.map((p) => ({
            blob: p.blob,
            maxScore: p.maxScore,
            questionType: p.questionType
          }))
        : await Promise.all(
            crops.value.map(async (crop) => ({
              blob: await cropToBlob(crop),
              maxScore: crop.maxScore,
              questionType: crop.questionType
            }))
          )

    await RemoteService.segmentExam(props.examId, items)
    info.value = 'Segmentação confirmada.'
    emit('segmented')
    await router.push({ name: 'exams' })
  } catch (e: unknown) {
    error.value = RemoteService.consumeError(e)
  } finally {
    confirming.value = false
  }
}
</script>

<style scoped>
.page-stage {
  position: relative;
  display: inline-block;
  max-width: 100%;
  user-select: none;
  cursor: crosshair;
  line-height: 0;
  outline: 1px solid rgba(0, 0, 0, 0.35);
}

.page-image {
  display: block;
  max-width: 100%;
  width: 100%;
  height: auto;
  pointer-events: none;
  margin: 0;
  vertical-align: top;
}

.crop-box {
  position: absolute;
  border: 2px solid rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.15);
  box-sizing: border-box;
  pointer-events: none;
}

.crop-box--draft {
  border-style: dashed;
  background: rgba(var(--v-theme-primary), 0.08);
}

.crop-label {
  position: absolute;
  top: 0;
  left: 0;
  padding: 0 4px;
  font-size: 12px;
  line-height: 18px;
  color: white;
  background: rgb(var(--v-theme-primary));
}
</style>
