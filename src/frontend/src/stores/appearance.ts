import { defineStore } from 'pinia'
import type DeiError from '@/models/DeiError'
import { ref, reactive, onMounted } from 'vue'

export const useAppearanceStore = defineStore('appearance', {
  state: () => ({
    currentTheme: 'light',
    isDarkTheme: false,
    errorMessagesStack: reactive([]) as string[],
    loading: ref(false),
    windowWidth: ref(window.innerWidth)
  }),
  getters: {
    isMobile(): boolean {
      return this.windowWidth < 1263
    },
    isStackEmpty(): boolean {
      return this.errorMessagesStack.length === 0
    },
    currentErrorMessage(): string | null {
      return this.errorMessagesStack[this.errorMessagesStack.length - 1] ?? null
    }
  },

  actions: {
    toggleTheme() {
      this.currentTheme = this.currentTheme === 'light' ? 'dark' : 'light'
      this.isDarkTheme = this.currentTheme === 'dark'
    },
    pushError(err: DeiError) {
      this.errorMessagesStack.push(err.message)
    },
    popError(): string | null {
      return this.errorMessagesStack.pop() ?? null
    },
    clearErrors() {
      this.errorMessagesStack = reactive([])
    }
  },
  persist: true
})