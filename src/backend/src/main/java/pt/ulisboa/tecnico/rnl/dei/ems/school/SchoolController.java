package pt.ulisboa.tecnico.rnl.dei.ems.school;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.rnl.dei.ems.school.dto.SchoolDto;
import pt.ulisboa.tecnico.rnl.dei.ems.school.service.SchoolService;

@RestController
public class SchoolController {
	@Autowired
	private SchoolService schoolService;

	@GetMapping("/schools")
	@PreAuthorize("hasAuthority('SCHOOL_READ')")
	public List<SchoolDto> getSchools() {
		return schoolService.getSchools();
	}

	@PostMapping("/schools")
	@PreAuthorize("hasAuthority('SCHOOL_CREATE')")
	public SchoolDto createSchool(@RequestBody SchoolDto schoolDto) {
		return schoolService.createSchool(schoolDto);
	}

	@GetMapping("/schools/{id}")
	@PreAuthorize("hasAuthority('SCHOOL_READ')")
	public SchoolDto getSchool(@PathVariable long id) {
		return schoolService.getSchool(id);
	}

	@PutMapping("/schools/{id}")
	@PreAuthorize("hasAuthority('SCHOOL_UPDATE')")
	public SchoolDto updateSchool(@PathVariable long id, @RequestBody SchoolDto schoolDto) {
		return schoolService.updateSchool(id, schoolDto);
	}

	@DeleteMapping("/schools/{id}")
	@PreAuthorize("hasAuthority('SCHOOL_DELETE')")
	public void deleteSchool(@PathVariable long id) {
		schoolService.deleteSchool(id);
	}
}
