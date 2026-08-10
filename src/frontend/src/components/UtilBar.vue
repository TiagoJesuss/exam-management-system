<template>
  <v-app-bar color="secondary" :height="36" class="px-2">
    <v-toolbar-items>
      <v-btn
        href="https://dei.tecnico.ulisboa.pt/"
        selected-class="no-active"
        class="dei-title"
        size="small"
      >
        Departamento de Engenharia Informática
      </v-btn>
    </v-toolbar-items>

    <v-spacer />

    <!-- Impersonation banner: only visible while acting as another account -->
    <v-chip
      v-if="authStore.isImpersonating"
      color="warning"
      size="small"
      variant="flat"
      class="me-3"
    >
      <v-icon start icon="mdi-account-switch" />
      A personificar {{ authStore.user?.name }}
      <v-btn size="x-small" variant="text" class="ms-1" @click="stopImpersonation">terminar</v-btn>
    </v-chip>

    <!-- Logged-in identity (read-only; the role comes from the account) -->
    <span v-if="authStore.user" class="text-caption">
      {{ authStore.user.name }} — {{ roleLabel(authStore.user.role) }}
    </span>

    <v-spacer />

    <v-toolbar-items class="align-center">
      <DarkModeSwitch />
    </v-toolbar-items>

    <v-toolbar-items class="ms-2">
      <v-btn size="small" variant="text" @click="logout">
        Terminar sessão
        <v-icon size="small" class="ms-1" icon="mdi-logout"></v-icon>
      </v-btn>
    </v-toolbar-items>
  </v-app-bar>
</template>

<script setup lang="ts">
import DarkModeSwitch from './DarkModeSwitch.vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

// Friendly labels for the backend role enum.
const ROLE_LABELS: Record<string, string> = {
  ADMINISTRATOR: 'Administrador',
  SCHOOL_STAFF: 'Funcionário Escolar',
  TEACHER: 'Professor',
  STUDENT: 'Aluno'
}
const roleLabel = (role: string) => ROLE_LABELS[role] ?? role

const logout = () => {
  authStore.logout()
  router.push({ name: 'login' })
}

const stopImpersonation = () => {
  authStore.stopImpersonation()
  router.push({ name: 'people' })
}
</script>

<style scoped>
.dei-title:hover {
  background-color: transparent !important;
}
</style>
