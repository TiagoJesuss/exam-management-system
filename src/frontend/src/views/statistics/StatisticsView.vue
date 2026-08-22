<template>
  <div class="text-left">
    <h2 class="text-h4 mb-4">Estatísticas</h2>

    <div v-if="loading" class="d-flex justify-center py-12">
      <v-progress-circular indeterminate color="primary" size="48" />
    </div>

    <template v-else-if="dashboard">
      <v-row class="mb-4">
        <v-col cols="12" sm="6" md="3">
          <v-card variant="tonal" color="primary">
            <v-card-text>
              <div class="text-caption text-medium-emphasis">Exames</div>
              <div class="text-h4">{{ totalExams }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card variant="tonal" color="secondary">
            <v-card-text>
              <div class="text-caption text-medium-emphasis">Correcção</div>
              <div class="text-h4">{{ gradingPercentLabel }}</div>
              <div class="text-caption">
                {{ dashboard.gradingProgress?.gradedCount ?? 0 }} /
                {{ dashboard.gradingProgress?.totalCount ?? 0 }} fragmentos
              </div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card variant="tonal">
            <v-card-text>
              <div class="text-caption text-medium-emphasis">Pedidos de revisão</div>
              <div class="text-h4">{{ dashboard.reviewVolume?.total ?? 0 }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card variant="tonal">
            <v-card-text>
              <div class="text-caption text-medium-emphasis">Disciplinas com média</div>
              <div class="text-h4">{{ dashboard.averagesBySubject?.length ?? 0 }}</div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-row>
        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>Exames por estado</v-card-title>
            <v-card-text>
              <Bar v-if="examsChartData" :data="examsChartData" :options="barOptions" />
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>Média por disciplina</v-card-title>
            <v-card-text>
              <Bar
                v-if="averagesChartData"
                :data="averagesChartData"
                :options="averageBarOptions"
              />
              <v-alert v-else type="info" variant="tonal" density="compact">
                Ainda não há exames com nota final.
              </v-alert>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-card class="mt-4">
        <v-card-title>Volume de revisões</v-card-title>
        <v-card-text class="d-flex flex-wrap ga-2">
          <v-chip
            v-for="item in dashboard.reviewVolume?.byStatus ?? []"
            :key="item.label"
            variant="tonal"
          >
            {{ item.label }}: {{ item.count }}
          </v-chip>
        </v-card-text>
      </v-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Title,
  Tooltip
} from 'chart.js'
import RemoteService from '@/services/RemoteService'
import type StatisticsDashboardDto from '@/models/StatisticsDashboardDto'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

const loading = ref(true)
const dashboard = ref<StatisticsDashboardDto | null>(null)

const totalExams = computed(() =>
  (dashboard.value?.examsByState ?? []).reduce((sum, row) => sum + (row.count ?? 0), 0)
)

const gradingPercentLabel = computed(() => {
  const p = dashboard.value?.gradingProgress?.percent ?? 0
  return `${p}%`
})

const barOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: { legend: { display: false } }
}

const averageBarOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: { legend: { display: false } },
  scales: {
    y: { beginAtZero: true }
  }
}

const examsChartData = computed(() => {
  const rows = dashboard.value?.examsByState ?? []
  if (rows.length === 0) return null
  return {
    labels: rows.map((r) => r.label ?? ''),
    datasets: [
      {
        label: 'Exames',
        backgroundColor: '#1976D2',
        data: rows.map((r) => r.count ?? 0)
      }
    ]
  }
})

const averagesChartData = computed(() => {
  const rows = dashboard.value?.averagesBySubject ?? []
  if (rows.length === 0) return null
  return {
    labels: rows.map((r) => r.subjectCode ?? ''),
    datasets: [
      {
        label: 'Média',
        backgroundColor: '#00897B',
        data: rows.map((r) => r.average ?? 0)
      }
    ]
  }
})

onMounted(async () => {
  loading.value = true
  try {
    dashboard.value = await RemoteService.getStatistics()
  } catch (error) {
    RemoteService.consumeError(error)
  } finally {
    loading.value = false
  }
})
</script>
