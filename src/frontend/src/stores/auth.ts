import { defineStore } from 'pinia'
import RemoteService from '@/services/RemoteService'
import type AuthUser from '@/models/AuthUser'

interface StashedSession {
  token: string
  user: AuthUser
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null as string | null,
    user: null as AuthUser | null,
    // While impersonating, the original admin session is stashed here so we can
    // return to it instantly (also survives reloads via persisted state).
    original: null as StashedSession | null
  }),

  getters: {
    isAuthenticated(): boolean {
      return !!this.token
    },
    isAdmin(): boolean {
      return this.user?.role === 'ADMINISTRATOR'
    },
    isImpersonating(): boolean {
      return !!this.original
    },
    // Usage: authStore.hasPermission('PERSON_DELETE')
    hasPermission(): (permission: string) => boolean {
      return (permission: string) => this.user?.permissions?.includes(permission) ?? false
    }
  },

  actions: {
    async login(email: string, password: string) {
      const res = await RemoteService.login(email, password)
      this.token = res.token
      this.user = res.user
      this.original = null
    },

    async fetchMe() {
      if (!this.token) return
      this.user = await RemoteService.getCurrentUser()
    },

    // Admin-only: become another person. The admin session is stashed first.
    async impersonate(personId: number) {
      if (!this.token || !this.user) return
      const stash: StashedSession = { token: this.token, user: this.user }
      const res = await RemoteService.impersonate(personId)
      this.token = res.token
      this.user = res.user
      this.original = stash
    },

    // Return to the stashed admin session.
    stopImpersonation() {
      if (!this.original) return
      this.token = this.original.token
      this.user = this.original.user
      this.original = null
    },

    logout() {
      this.token = null
      this.user = null
      this.original = null
    }
  },

  persist: true
})
