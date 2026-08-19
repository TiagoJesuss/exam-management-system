export default class SubjectDto {
  id?: number
  code?: string
  name?: string

  constructor(obj?: Partial<SubjectDto>) {
		Object.assign(this, obj)
	}
}
