import axios from 'axios'
import type { AxiosResponse } from 'axios'
import { useAppearanceStore } from '@/stores/appearance'
import { useAuthStore } from '@/stores/auth'
import DeiError from '@/models/DeiError'
import type PersonDto from '@/models/PersonDto'
import type AuthUser from '@/models/AuthUser'

const httpClient = axios.create()
httpClient.defaults.timeout = 50000
httpClient.defaults.baseURL = import.meta.env.VITE_ROOT_API
httpClient.defaults.headers.post['Content-Type'] = 'application/json'

export interface LoginResponse {
  token: string
  expiresInMs: number
  user: AuthUser
}

export default class RemoteServices {
  static async getPeople(): Promise<PersonDto[]> {
    return httpClient.get('/people')
  }

  static async createPerson(person: PersonDto): Promise<PersonDto> {
    return httpClient.post('/people', person)
  }

  // --- Authentication ---
  static async login(email: string, password: string): Promise<LoginResponse> {
    return httpClient.post('/auth/login', { email, password })
  }

  static async getCurrentUser(): Promise<AuthUser> {
    return httpClient.get('/auth/me')
  }

  static async impersonate(personId: number): Promise<LoginResponse> {
    return httpClient.post(`/auth/impersonate/${personId}`)
  }

  static async stopImpersonation(): Promise<LoginResponse> {
    return httpClient.post('/auth/impersonate/stop')
  }

  static async errorMessage(error: any): Promise<string> {
    if (error.message === 'Network Error') {
      return 'Unable to connect to the server'
    } else if (error.message.split(' ')[0] === 'timeout') {
      return 'Request timeout - Server took too long to respond'
    } else {
      return error.response?.data?.message ?? 'Unknown Error'
    }
  }

  static async handleError(error: any): Promise<never> {
    // An expired/invalid token yields 401: drop the session so the router
    // guard sends the user back to the login page.
    if (error.response?.status === 401) {
      useAuthStore().logout()
    }
    const deiErr = new DeiError(
      await RemoteServices.errorMessage(error),
      error.response?.data?.code ?? -1
    )
    const appearance = useAppearanceStore()
    appearance.pushError(deiErr)
    appearance.loading = false
    throw deiErr
  }
}

// Attach the JWT (if any) to every outgoing request.
httpClient.interceptors.request.use((request) => {
  const auth = useAuthStore()
  if (auth.token) {
    request.headers.Authorization = `Bearer ${auth.token}`
  }
  return request
}, RemoteServices.handleError)
httpClient.interceptors.response.use((response) => response.data, RemoteServices.handleError)
