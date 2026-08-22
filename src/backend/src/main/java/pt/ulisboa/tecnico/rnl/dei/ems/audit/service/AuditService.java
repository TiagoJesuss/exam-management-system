package pt.ulisboa.tecnico.rnl.dei.ems.audit.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import pt.ulisboa.tecnico.rnl.dei.ems.audit.domain.AuditAction;
import pt.ulisboa.tecnico.rnl.dei.ems.audit.domain.AuditLog;
import pt.ulisboa.tecnico.rnl.dei.ems.audit.dto.AuditLogDto;
import pt.ulisboa.tecnico.rnl.dei.ems.audit.repository.AuditLogRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.security.JwtService;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;

@Service
@Transactional
public class AuditService {

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private JwtService jwtService;

	public void log(AuditAction action, String message, String entityType, Long entityId, String details) {
		Person actor = SecurityUtils.currentPerson();
		Long actorId = actor != null ? actor.getId() : null;
		String actorEmail = actor != null ? actor.getEmail() : null;
		String actorName = actor != null ? actor.getName() : null;

		String fullMessage = appendImpersonationNote(message);

		auditLogRepository.save(new AuditLog(
				Instant.now(),
				actorId,
				actorEmail,
				actorName,
				action,
				fullMessage,
				entityType,
				entityId,
				details));
	}

	@Transactional(readOnly = true)
	public List<AuditLogDto> listAll() {
		return auditLogRepository.findAllByOrderByTimestampDesc().stream()
				.map(AuditLogDto::new)
				.toList();
	}

	private String appendImpersonationNote(String message) {
		Claims claims = parseCurrentTokenClaims();
		if (claims == null) {
			return message;
		}
		Object flag = claims.get(JwtService.CLAIM_IMPERSONATING);
		boolean impersonating = flag != null && Boolean.parseBoolean(flag.toString());
		if (!impersonating) {
			return message;
		}
		String adminName = claims.get(JwtService.CLAIM_IMPERSONATOR_NAME, String.class);
		if (adminName == null || adminName.isBlank()) {
			adminName = claims.get(JwtService.CLAIM_IMPERSONATOR_EMAIL, String.class);
		}
		if (adminName == null || adminName.isBlank()) {
			adminName = "admin";
		}
		return message + " (personificado por " + adminName + ")";
	}

	private Claims parseCurrentTokenClaims() {
		ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return null;
		}
		HttpServletRequest request = attrs.getRequest();
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			return null;
		}
		try {
			return jwtService.extractAllClaims(header.substring(7));
		} catch (Exception e) {
			return null;
		}
	}
}
