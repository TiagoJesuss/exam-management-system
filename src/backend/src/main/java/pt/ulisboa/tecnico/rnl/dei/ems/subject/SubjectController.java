package pt.ulisboa.tecnico.rnl.dei.ems.subject;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.rnl.dei.ems.subject.dto.SubjectDto;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.service.SubjectService;

@RestController
public class SubjectController {
	@Autowired
	private SubjectService subjectService;

	@GetMapping("/subjects")
	@PreAuthorize("hasAuthority('SUBJECT_READ')")
	public List<SubjectDto> getSubjects() {
		return subjectService.getSubjects();
	}

	@PostMapping("/subjects")
	@PreAuthorize("hasAuthority('SUBJECT_CREATE')")
	public SubjectDto createSubject(@RequestBody SubjectDto subjectDto) {
		return subjectService.createSubject(subjectDto);
	}

	@GetMapping("/subjects/{id}")
	@PreAuthorize("hasAuthority('SUBJECT_READ')")
	public SubjectDto getSubject(@PathVariable long id) {
		return subjectService.getSubject(id);
	}

	@PutMapping("/subjects/{id}")
	@PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
	public SubjectDto updateSubject(@PathVariable long id, @RequestBody SubjectDto subjectDto) {
		return subjectService.updateSubject(id, subjectDto);
	}

	@DeleteMapping("/subjects/{id}")
	@PreAuthorize("hasAuthority('SUBJECT_DELETE')")
	public void deleteSubject(@PathVariable long id) {
		subjectService.deleteSubject(id);
	}
}
