package pt.ulisboa.tecnico.rnl.dei.ems.audit.dto;

import java.time.Instant;

import pt.ulisboa.tecnico.rnl.dei.ems.audit.domain.AuditLog;

public record AuditLogDto(
		Long id,
		Instant timestamp,
		Long actorId,
		String actorEmail,
		String actorName,
		String action,
		String message,
		String entityType,
		Long entityId,
		String details) {

	public AuditLogDto(AuditLog log) {
		this(
				log.getId(),
				log.getTimestamp(),
				log.getActorId(),
				log.getActorEmail(),
				log.getActorName(),
				log.getAction().name(),
				log.getMessage(),
				log.getEntityType(),
				log.getEntityId(),
				log.getDetails());
	}
}
