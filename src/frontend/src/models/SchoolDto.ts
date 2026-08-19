export default class SchoolDto {
  id?: number
  code?: string
  name?: string
  region?: string

  constructor(obj?: Partial<SchoolDto>) {
    Object.assign(this, obj)
  }
}
