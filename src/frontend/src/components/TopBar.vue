<template>
  <UtilBar />
  <NavBar :navbarItems="navbarItems" />
</template>

<script setup lang="ts">
import UtilBar from '@/components/UtilBar.vue'
import NavBar from '@/components/NavBar.vue'
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'
import { navItems } from '@/navigation/navItems'

const auth = useAuthStore()

const navbarItems = computed(() => navItems.filter(item => auth.hasPermission(item.permission)))

</script>
