package pt.ulisboa.tecnico.rnl.dei.ems.evaluate;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.dto.EvaluateInboxDto;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.dto.GradeRequest;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.service.EvaluateService;

@RestController
public class EvaluateController {
	@Autowired
	private EvaluateService evaluateService;

	@GetMapping("/evaluates/mine")
	@PreAuthorize("hasAuthority('EVALUATE_READ')")
	public List<EvaluateInboxDto> getMyInbox() {
		return evaluateService.getMyInbox();
	}

	@GetMapping("/evaluates/{id}")
	@PreAuthorize("hasAuthority('EVALUATE_READ')")
	public EvaluateInboxDto getEvaluate(@PathVariable long id) {
		return evaluateService.getMineAndStart(id);
	}

	@GetMapping("/evaluates/{id}/image")
	@PreAuthorize("hasAuthority('EVALUATE_READ')")
	public ResponseEntity<Resource> getEvaluateImage(@PathVariable long id) {
		Path path = evaluateService.getOwnedImagePath(id);
		Resource resource = new FileSystemResource(path);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"evaluate-" + id + ".png\"")
				.body(resource);
	}

	@PutMapping("/evaluates/{id}/grade")
	@PreAuthorize("hasAuthority('EVALUATE_GRADE')")
	public EvaluateInboxDto submitGrade(@PathVariable long id, @RequestBody GradeRequest request) {
		return evaluateService.submitGrade(id, request.grade());
	}
}
