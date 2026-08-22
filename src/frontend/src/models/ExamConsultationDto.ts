import type FragmentGradeDto from '@/models/FragmentGradeDto'

export default class ExamConsultationDto {
  examId?: number
  state?: string
  subjectCode?: string
  finalScore?: number | null
  reviewDeadline?: string | null
  consultationRequested?: boolean
  fragments?: FragmentGradeDto[]

  constructor(obj?: Partial<ExamConsultationDto>) {
    Object.assign(this, obj)
  }
}
