<template>
  <UtilBar />
  <NavBar :navbarItems="navbarItems" />
</template>

<script setup lang="ts">
import UtilBar from '@/components/UtilBar.vue'
import NavBar from '@/components/NavBar.vue'
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'

const auth = useAuthStore()

const allNavbarItems = [
  { name: 'Pessoal', path: '/people', icon: 'mdi-account-group', permission: 'PERSON_READ' },
  { name: 'Escolas', path: '/schools', icon: 'mdi-school', permission: 'SCHOOL_READ' },
  { name: 'Disciplinas', path: '/subjects', icon: 'mdi-book-open-variant', permission: 'SUBJECT_READ' },
  { name: 'Exames', path: '/exams', icon: 'mdi-file-document-multiple', permission: 'EXAM_READ' },
  { name: 'Correcções', path: '/grading', icon: 'mdi-clipboard-check', permission: 'EVALUATE_READ' },
  { name: 'Estatísticas', path: '/statistics', icon: 'mdi-chart-bar', permission: 'STATISTICS_READ' },
]

const navbarItems = computed(() => allNavbarItems.filter(item => auth.hasPermission(item.permission)))

</script>
