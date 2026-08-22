package pt.ulisboa.tecnico.rnl.dei.ems.audit;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.rnl.dei.ems.audit.dto.AuditLogDto;
import pt.ulisboa.tecnico.rnl.dei.ems.audit.service.AuditService;

@RestController
public class AuditController {

	@Autowired
	private AuditService auditService;

	@GetMapping("/audit-logs")
	@PreAuthorize("hasAuthority('AUDIT_READ')")
	public List<AuditLogDto> list() {
		return auditService.listAll();
	}
}
