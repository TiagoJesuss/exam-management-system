<template>
  <v-container class="d-flex justify-center align-center" style="min-height: 60vh">
    <v-card width="420" class="pa-6 elevation-6 text-left">
      <v-card-title class="text-h5 mb-1">Iniciar sessão</v-card-title>
      <v-card-subtitle class="mb-4">{{ appName }}</v-card-subtitle>

      <v-form @submit.prevent="onSubmit">
        <v-text-field
          v-model="email"
          label="Email"
          type="email"
          prepend-inner-icon="mdi-email"
          autocomplete="username"
          required
        ></v-text-field>
        <v-text-field
          v-model="password"
          label="Palavra-passe"
          type="password"
          prepend-inner-icon="mdi-lock"
          autocomplete="current-password"
          required
        ></v-text-field>

        <v-alert v-if="error" type="error" density="compact" class="mb-3">{{ error }}</v-alert>

        <v-btn type="submit" color="primary" block :loading="loading">Entrar</v-btn>
      </v-form>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const appName = import.meta.env.VITE_NAME

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const onSubmit = async () => {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    const redirect = route.query.redirect as string | undefined
    router.push(redirect ?? { name: 'home' })
  } catch (e: any) {
    error.value = e?.message ?? 'Falha na autenticação.'
  } finally {
    loading.value = false
  }
}
</script>
