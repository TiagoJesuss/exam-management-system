export default class EvaluateInboxDto {
  id?: number
  state?: string
  type?: string
  questionNumber?: number
  maxScore?: number
  subjectCode?: string
  grade?: number | null
  justification?: string | null
  previousGrade?: number | null

  constructor(obj?: Partial<EvaluateInboxDto>) {
    Object.assign(this, obj)
  }
}
