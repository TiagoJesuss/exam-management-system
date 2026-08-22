package pt.ulisboa.tecnico.rnl.dei.ems.school.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.audit.domain.AuditAction;
import pt.ulisboa.tecnico.rnl.dei.ems.audit.service.AuditService;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.school.domain.School;
import pt.ulisboa.tecnico.rnl.dei.ems.school.dto.SchoolDto;
import pt.ulisboa.tecnico.rnl.dei.ems.school.repository.SchoolRepository;

// Service class for managing School entities
@Service
@Transactional
public class SchoolService {

	@Autowired
	private SchoolRepository schoolRepository;

	@Autowired
	private AuditService auditService;

	private School fetchSchoolOrThrow(long id) {
		return schoolRepository.findById(id)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SCHOOL, Long.toString(id)));
	}

	@Transactional
	public List<SchoolDto> getSchools() {
		return schoolRepository.findAll().stream()
				.map(SchoolDto::new)
				.toList();
	}

	@Transactional
	public SchoolDto createSchool(SchoolDto schoolDto) {
		validateSchoolFields(schoolDto);

		if (schoolRepository.existsByCode(schoolDto.code())) {
			throw new DEIException(ErrorMessage.SCHOOL_ALREADY_EXISTS, schoolDto.code());
		}

		School school = new School(
				schoolDto.name(),
				schoolDto.region(),
				schoolDto.code());
		School saved = schoolRepository.save(school);
		auditService.log(
				AuditAction.SCHOOL_CREATED,
				"Criou a escola " + saved.getCode() + ".",
				"School",
				saved.getId(),
				null);
		return new SchoolDto(saved);
	}

	@Transactional
	public SchoolDto getSchool(long id) {
		return new SchoolDto(fetchSchoolOrThrow(id));
	}

	@Transactional
	public SchoolDto updateSchool(long id, SchoolDto schoolDto) {
		validateSchoolFields(schoolDto);

		if (schoolRepository.existsByCode(schoolDto.code()) && schoolRepository.findByCode(schoolDto.code()).get().getId() != id) {
			throw new DEIException(ErrorMessage.SCHOOL_ALREADY_EXISTS, schoolDto.code());
		}

		School school = fetchSchoolOrThrow(id);
		String oldName = school.getName();
		String oldRegion = school.getRegion();
		String oldCode = school.getCode();

		school.setName(schoolDto.name());
		school.setRegion(schoolDto.region());
		school.setCode(schoolDto.code());
		School saved = schoolRepository.save(school);

		List<String> changed = new ArrayList<>();
		if (!Objects.equals(oldName, saved.getName())) {
			changed.add("nome");
		}
		if (!Objects.equals(oldRegion, saved.getRegion())) {
			changed.add("região");
		}
		if (!Objects.equals(oldCode, saved.getCode())) {
			changed.add("código");
		}

		if (!changed.isEmpty()) {
			auditService.log(
					AuditAction.SCHOOL_UPDATED,
					"Editou a escola " + saved.getCode() + ": " + String.join(", ", changed) + ".",
					"School",
					saved.getId(),
					null);
		}

		return new SchoolDto(saved);
	}

	private void validateSchoolFields(SchoolDto schoolDto) {
		if (schoolDto.name() == null || schoolDto.name().isBlank()
				|| schoolDto.code() == null || schoolDto.code().isBlank()
				|| schoolDto.region() == null || schoolDto.region().isBlank()) {
			throw new DEIException(ErrorMessage.SCHOOL_FIELDS_NOT_VALID);
		}
	}

	@Transactional
	public void deleteSchool(long id) {
		School school = fetchSchoolOrThrow(id);
		String code = school.getCode();
		schoolRepository.deleteById(id);
		auditService.log(
				AuditAction.SCHOOL_DELETED,
				"Apagou a escola " + code + " (id " + id + ").",
				"School",
				id,
				null);
	}
}
