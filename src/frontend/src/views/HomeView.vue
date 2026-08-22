<template>
  <div class="text-left">
    <h1 class="text-h4 mb-2">{{ appName }}</h1>
    <p class="text-medium-emphasis mb-6">
      Sistema de gestão de exames nacionais — escolha uma secção para continuar.
    </p>

    <v-row v-if="availableItems.length > 0">
      <v-col
        v-for="item in availableItems"
        :key="item.path"
        cols="12"
        sm="6"
        md="4"
        lg="3"
      >
        <v-card
          :to="item.path"
          hover
          class="h-100"
        >
          <v-card-text class="d-flex align-center ga-3">
            <v-icon :icon="item.icon" size="32" color="primary" />
            <span class="text-h6">{{ item.name }}</span>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-alert v-else type="info" variant="tonal">
      Não tem secções disponíveis com a conta actual.
    </v-alert>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { navItems } from '@/navigation/navItems'

const appName = import.meta.env.VITE_NAME
const auth = useAuthStore()

const availableItems = computed(() =>
  navItems.filter((item) => auth.hasPermission(item.permission))
)
</script>
