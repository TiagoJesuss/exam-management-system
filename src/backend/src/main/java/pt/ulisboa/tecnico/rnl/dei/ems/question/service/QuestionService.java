package pt.ulisboa.tecnico.rnl.dei.ems.question.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;
import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;
import pt.ulisboa.tecnico.rnl.dei.ems.question.dto.QuestionDto;
import pt.ulisboa.tecnico.rnl.dei.ems.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;

// Service class for managing Question entities
@Service
@Transactional
public class QuestionService {

	@Autowired
	private QuestionRepository questionRepository;

	private Question fetchQuestionOrThrow(long id) {
		return questionRepository.findById(id)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_QUESTION, Long.toString(id)));
	}

	@Transactional(readOnly = true)
	public List<QuestionDto> getQuestions() {
		Person current = requireCurrentPerson();
		return questionRepository.findAll().stream()
				.filter(question -> canAccessQuestion(current, question))
				.map(QuestionDto::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public QuestionDto getQuestion(long id) {
		Question question = fetchQuestionOrThrow(id);
		assertCanAccessQuestion(question);
		return new QuestionDto(question);
	}

	private Person requireCurrentPerson() {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}
		return current;
	}

	private void assertCanAccessQuestion(Question question) {
		Person current = requireCurrentPerson();
		if (!canAccessQuestion(current, question)) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
	}

	private boolean canAccessQuestion(Person current, Question question) {
		if (current.getType() == PersonType.ADMINISTRATOR) {
			return true;
		}
		if (current.getType() == PersonType.SCHOOL_STAFF) {
			return current.getSchool() != null
					&& question.getExam().getStudent().getSchool() != null
					&& current.getSchool().getId().equals(question.getExam().getStudent().getSchool().getId());
		}
		return false;
	}
}
