package pt.ulisboa.tecnico.rnl.dei.ems.subject.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.dto.SubjectDto;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.repository.SubjectRepository;

// Service class for managing Subject entities
@Service
@Transactional
public class SubjectService {

	@Autowired
	private SubjectRepository subjectRepository;

	private Subject fetchSubjectOrThrow(long id) {
		return subjectRepository.findById(id)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SUBJECT, Long.toString(id)));
	}

	@Transactional
	public List<SubjectDto> getSubjects() {
		return subjectRepository.findAll().stream()
				.map(SubjectDto::new)
				.toList();
	}

	@Transactional
	public SubjectDto createSubject(SubjectDto subjectDto) {
		validateSubjectFields(subjectDto);

		if (subjectRepository.existsByCode(subjectDto.code())) {
			throw new DEIException(ErrorMessage.SUBJECT_ALREADY_EXISTS, subjectDto.code());
		}

		Subject subject = new Subject(
				subjectDto.name(),
				subjectDto.code());
		return new SubjectDto(subjectRepository.save(subject));
	}

	@Transactional
	public SubjectDto getSubject(long id) {
		return new SubjectDto(fetchSubjectOrThrow(id));
	}

	@Transactional
	public SubjectDto updateSubject(long id, SubjectDto subjectDto) {
		validateSubjectFields(subjectDto);

		if (subjectRepository.existsByCode(subjectDto.code()) && subjectRepository.findByCode(subjectDto.code()).get().getId() != id) {
			throw new DEIException(ErrorMessage.SUBJECT_ALREADY_EXISTS, subjectDto.code());
		}

		Subject subject = fetchSubjectOrThrow(id);

		subject.setCode(subjectDto.code());
		subject.setName(subjectDto.name());

		return new SubjectDto(subjectRepository.save(subject));
	}

	private void validateSubjectFields(SubjectDto subjectDto) {
		if (subjectDto.name() == null || subjectDto.name().isBlank()
				|| subjectDto.code() == null || subjectDto.code().isBlank()) {
			throw new DEIException(ErrorMessage.SUBJECT_FIELDS_NOT_VALID);
		}
	}

	@Transactional
	public void deleteSubject(long id) {
		fetchSubjectOrThrow(id); // ensure exists

		subjectRepository.deleteById(id);
	}
}
