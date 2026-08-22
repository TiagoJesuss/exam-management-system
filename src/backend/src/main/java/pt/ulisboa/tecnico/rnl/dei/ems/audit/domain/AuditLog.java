package pt.ulisboa.tecnico.rnl.dei.ems.audit.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "timestamp", nullable = false)
	private Instant timestamp;

	@Column(name = "actor_id")
	private Long actorId;

	@Column(name = "actor_email", length = 255)
	private String actorEmail;

	@Column(name = "actor_name", length = 255)
	private String actorName;

	@Column(name = "action", nullable = false)
	@Enumerated(EnumType.STRING)
	private AuditAction action;

	@Column(name = "message", nullable = false, length = 2000)
	private String message;

	@Column(name = "entity_type", length = 100)
	private String entityType;

	@Column(name = "entity_id")
	private Long entityId;

	@Column(name = "details", length = 1000)
	private String details;

	protected AuditLog() {
	}

	public AuditLog(
			Instant timestamp,
			Long actorId,
			String actorEmail,
			String actorName,
			AuditAction action,
			String message,
			String entityType,
			Long entityId,
			String details) {
		this.timestamp = timestamp;
		this.actorId = actorId;
		this.actorEmail = actorEmail;
		this.actorName = actorName;
		this.action = action;
		this.message = message;
		this.entityType = entityType;
		this.entityId = entityId;
		this.details = details;
	}
}
