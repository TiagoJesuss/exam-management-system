export default class FragmentGradeDto {
  questionNumber?: number
  maxScore?: number
  grade?: number | null

  constructor(obj?: Partial<FragmentGradeDto>) {
    Object.assign(this, obj)
  }
}
