export default class AuditLogDto {
  id?: number
  timestamp?: string
  actorId?: number
  actorEmail?: string
  actorName?: string
  action?: string
  message?: string
  entityType?: string
  entityId?: number
  details?: string

  constructor(obj?: Partial<AuditLogDto>) {
    Object.assign(this, obj)
  }
}
