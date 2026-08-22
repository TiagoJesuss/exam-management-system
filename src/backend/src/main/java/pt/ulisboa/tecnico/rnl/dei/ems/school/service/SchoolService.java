package pt.ulisboa.tecnico.rnl.dei.ems.school.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
		return new SchoolDto(schoolRepository.save(school));
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

		school.setName(schoolDto.name());
		school.setRegion(schoolDto.region());
		school.setCode(schoolDto.code());
		return new SchoolDto(schoolRepository.save(school));
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
		fetchSchoolOrThrow(id); // ensure exists

		schoolRepository.deleteById(id);
	}
}
