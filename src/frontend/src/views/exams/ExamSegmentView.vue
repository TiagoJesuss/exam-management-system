<template>
  <v-row align="center">
    <v-col>
      <h2 class="text-left ml-1">Segmentar exame #{{ examId }}</h2>
      <p class="text-medium-emphasis ml-1 mb-0">
        Escolha o modo Páginas (uma pergunta por página) ou Recortar (áreas manuais). Ajuste a cotação máxima se necessário.
      </p>
    </v-col>
    <v-col cols="auto">
      <v-btn variant="text" :to="{ name: 'exams' }">Voltar</v-btn>
    </v-col>
  </v-row>

  <ExamSegmentPanel :exam-id="examId" />
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import ExamSegmentPanel from './ExamSegmentPanel.vue'

const route = useRoute()
const examId = Number(route.params.id)
</script>
