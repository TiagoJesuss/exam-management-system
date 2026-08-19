import axios from 'axios'
import type { AxiosResponse } from 'axios'
import { useAppearanceStore } from '@/stores/appearance'
import { useAuthStore } from '@/stores/auth'
import DeiError from '@/models/DeiError'
import type PersonDto from '@/models/PersonDto'
import type SchoolDto from '@/models/SchoolDto'
import type SubjectDto from '@/models/SubjectDto'
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
  // --- People ---
  static async getPeople(): Promise<PersonDto[]> {
    return httpClient.get('/people')
  }

  static async createPerson(person: PersonDto): Promise<PersonDto> {
    return httpClient.post('/people', person)
  }

  static async getPerson(id: number): Promise<PersonDto> {
    return httpClient.get(`/people/${id}`)
  }

  static async updatePerson(id: number, person: PersonDto): Promise<PersonDto> {
    return httpClient.put(`/people/${id}`, person)
  }

  static async deletePerson(id: number): Promise<void> {
    return httpClient.delete(`/people/${id}`)
  }

  // --- Schools ---
  static async getSchools(): Promise<SchoolDto[]> {
    return httpClient.get('/schools')
  }

  static async createSchool(school: SchoolDto): Promise<SchoolDto> {
    return httpClient.post('/schools', school)
  }

  static async getSchool(id: number): Promise<SchoolDto> {
    return httpClient.get(`/schools/${id}`)
  }

  static async updateSchool(id: number, school: SchoolDto): Promise<SchoolDto> {
    return httpClient.put(`/schools/${id}`, school)
  }

  static async deleteSchool(id: number): Promise<void> {
    return httpClient.delete(`/schools/${id}`)
  }

  // --- Subjects ---
  static async getSubjects(): Promise<SubjectDto[]> {
    return httpClient.get('/subjects')
  }

  static async createSubject(subject: SubjectDto): Promise<SubjectDto> {
    return httpClient.post('/subjects', subject)
  }

  static async getSubject(id: number): Promise<SubjectDto> {
    return httpClient.get(`/subjects/${id}`)
  }

  static async updateSubject(id: number, subject: SubjectDto): Promise<SubjectDto> {
    return httpClient.put(`/subjects/${id}`, subject)
  }

  static async deleteSubject(id: number): Promise<void> {
    return httpClient.delete(`/subjects/${id}`)
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
