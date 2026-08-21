package pt.ulisboa.tecnico.rnl.dei.ems.question.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;
import pt.ulisboa.tecnico.rnl.dei.ems.question.dto.QuestionDto;
import pt.ulisboa.tecnico.rnl.dei.ems.question.repository.QuestionRepository;

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

	@Transactional
	public List<QuestionDto> getQuestions() {
		return questionRepository.findAll().stream()
				.map(QuestionDto::new)
				.toList();
	}

	@Transactional
	public QuestionDto getQuestion(long id) {
		return new QuestionDto(fetchQuestionOrThrow(id));
	}
}
