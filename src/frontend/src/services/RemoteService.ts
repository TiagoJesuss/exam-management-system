import axios from 'axios'
import type { AxiosResponse } from 'axios'
import { useAppearanceStore } from '@/stores/appearance'
import { useAuthStore } from '@/stores/auth'
import DeiError from '@/models/DeiError'
import type PersonDto from '@/models/PersonDto'
import type SchoolDto from '@/models/SchoolDto'
import type SubjectDto from '@/models/SubjectDto'
import type ExamDto from '@/models/ExamDto'
import type ExamConsultationDto from '@/models/ExamConsultationDto'
import type EvaluateInboxDto from '@/models/EvaluateInboxDto'
import type ReviewRequestDto from '@/models/ReviewRequestDto'
import type StatisticsDashboardDto from '@/models/StatisticsDashboardDto'
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

  // --- Exams ---
  static async getExams(): Promise<ExamDto[]> {
    return httpClient.get('/exams')
  }

  static async getMyExams(): Promise<ExamDto[]> {
    return httpClient.get('/exams/mine')
  }

  static async getExamConsultation(id: number): Promise<ExamConsultationDto> {
    return httpClient.get(`/exams/${id}/consultation`)
  }

  static async requestConsultation(id: number): Promise<ExamDto> {
    return httpClient.post(`/exams/${id}/request-consultation`)
  }

  static async getReviewRequests(examId: number): Promise<ReviewRequestDto[]> {
    return httpClient.get(`/exams/${examId}/review-requests`)
  }

  static async submitReviewRequest(
    examId: number,
    questionNumber: number,
    justification: string
  ): Promise<ReviewRequestDto> {
    return httpClient.post(`/exams/${examId}/review-requests`, {
      questionNumber,
      justification
    })
  }

  static async uploadExam(file: File, studentId: number, subjectCode: string): Promise<ExamDto> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('studentId', String(studentId))
    formData.append('subjectCode', subjectCode)
    return httpClient.post('/exams', formData, {
      headers: { 'Content-Type': undefined }
    })
  }

  static async getExamFile(id: number): Promise<Blob> {
    return httpClient.get(`/exams/${id}/file`, { responseType: 'blob' })
  }

  static async segmentExam(
    examId: number,
    pages: { blob: Blob; maxScore: number }[]
  ): Promise<ExamDto> {
    const formData = new FormData()
    for (const page of pages) {
      formData.append('files', page.blob, 'question.png')
      formData.append('maxScores', String(page.maxScore))
    }
    return httpClient.post(`/exams/${examId}/segment`, formData, {
      headers: { 'Content-Type': undefined }
    })
  }

  static async lockExams(): Promise<ExamDto[]> {
    return httpClient.post('/exams/lock')
  }

  static async distributeExams(): Promise<{ assignedCount: number; forcedSameSchoolCount: number }> {
    return httpClient.post('/exams/distribute')
  }

  static async closeReviewWindows(): Promise<{
    closedToFinal: number
    closedToReviewClosed: number
  }> {
    return httpClient.post('/exams/close-review-windows')
  }

  static async releaseExam(id: number): Promise<ExamDto> {
    return httpClient.post(`/exams/${id}/release`)
  }

  static async releaseExams(schoolCode: string, subjectCode: string): Promise<ExamDto[]> {
    return httpClient.post('/exams/release', null, {
      params: { schoolCode, subjectCode }
    })
  }

  static async publishGrades(
    schoolCode: string | null | undefined,
    subjectCode: string
  ): Promise<ExamDto[]> {
    return httpClient.post('/exams/publish-grades', null, {
      params: {
        ...(schoolCode ? { schoolCode } : {}),
        subjectCode
      }
    })
  }

  // --- Evaluates (grading) ---
  static async getMyEvaluates(): Promise<EvaluateInboxDto[]> {
    return httpClient.get('/evaluates/mine')
  }

  static async getEvaluate(id: number): Promise<EvaluateInboxDto> {
    return httpClient.get(`/evaluates/${id}`)
  }

  static async getEvaluateImage(id: number): Promise<Blob> {
    return httpClient.get(`/evaluates/${id}/image`, { responseType: 'blob' })
  }

  static async submitGrade(id: number, grade: number): Promise<EvaluateInboxDto> {
    return httpClient.put(`/evaluates/${id}/grade`, { grade })
  }

  // --- Statistics ---
  static async getStatistics(): Promise<StatisticsDashboardDto> {
    return httpClient.get('/statistics')
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
      return 'Não foi possível ligar ao servidor.'
    } else if (error.message.split(' ')[0] === 'timeout') {
      return 'Tempo de espera esgotado — o servidor demorou demasiado a responder.'
    } else {
      return error.response?.data?.message ?? 'Erro desconhecido.'
    }
  }

  static consumeError(error: unknown): string {
    const appearance = useAppearanceStore()
    if (error instanceof DeiError) {
      if (appearance.currentErrorMessage === error.message) {
        appearance.popError()
      }
      return error.message
    }
    return error instanceof Error ? error.message : 'Erro desconhecido.'
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
