export default class ExamDto {
  id?: number
  state?: string
  path?: string
  studentId?: number
  studentName?: string
  schoolCode?: string | null
  subjectCode?: string
  finalScore?: number | null

  constructor(obj?: Partial<ExamDto>) {
    Object.assign(this, obj)
  }
}
