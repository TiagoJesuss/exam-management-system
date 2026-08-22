package pt.ulisboa.tecnico.rnl.dei.ems.subject.service;

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
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.dto.SubjectDto;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.repository.SubjectRepository;

// Service class for managing Subject entities
@Service
@Transactional
public class SubjectService {

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private AuditService auditService;

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
		Subject saved = subjectRepository.save(subject);
		auditService.log(
				AuditAction.SUBJECT_CREATED,
				"Criou a disciplina " + saved.getCode() + ".",
				"Subject",
				saved.getId(),
				null);
		return new SubjectDto(saved);
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
		String oldName = subject.getName();
		String oldCode = subject.getCode();

		subject.setCode(subjectDto.code());
		subject.setName(subjectDto.name());
		Subject saved = subjectRepository.save(subject);

		List<String> changed = new ArrayList<>();
		if (!Objects.equals(oldName, saved.getName())) {
			changed.add("nome");
		}
		if (!Objects.equals(oldCode, saved.getCode())) {
			changed.add("código");
		}

		if (!changed.isEmpty()) {
			auditService.log(
					AuditAction.SUBJECT_UPDATED,
					"Editou a disciplina " + saved.getCode() + ": " + String.join(", ", changed) + ".",
					"Subject",
					saved.getId(),
					null);
		}

		return new SubjectDto(saved);
	}

	private void validateSubjectFields(SubjectDto subjectDto) {
		if (subjectDto.name() == null || subjectDto.name().isBlank()
				|| subjectDto.code() == null || subjectDto.code().isBlank()) {
			throw new DEIException(ErrorMessage.SUBJECT_FIELDS_NOT_VALID);
		}
	}

	@Transactional
	public void deleteSubject(long id) {
		Subject subject = fetchSubjectOrThrow(id);
		String code = subject.getCode();
		subjectRepository.deleteById(id);
		auditService.log(
				AuditAction.SUBJECT_DELETED,
				"Apagou a disciplina " + code + " (id " + id + ").",
				"Subject",
				id,
				null);
	}
}
