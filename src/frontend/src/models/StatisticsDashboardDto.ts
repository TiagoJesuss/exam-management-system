export interface CountByLabelDto {
  label?: string
  count?: number
}

export interface SubjectAverageDto {
  subjectCode?: string
  average?: number
  examCount?: number
}

export interface GradingProgressDto {
  gradedCount?: number
  totalCount?: number
  percent?: number
}

export interface ReviewVolumeDto {
  total?: number
  byStatus?: CountByLabelDto[]
}

export default class StatisticsDashboardDto {
  examsByState?: CountByLabelDto[]
  averagesBySubject?: SubjectAverageDto[]
  gradingProgress?: GradingProgressDto
  reviewVolume?: ReviewVolumeDto

  constructor(obj?: Partial<StatisticsDashboardDto>) {
    Object.assign(this, obj)
  }
}
