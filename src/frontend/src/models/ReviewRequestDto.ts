export default class ReviewRequestDto {
  id?: number
  questionNumber?: number
  justification?: string
  status?: string
  createdAt?: string

  constructor(obj?: Partial<ReviewRequestDto>) {
    Object.assign(this, obj)
  }
}
