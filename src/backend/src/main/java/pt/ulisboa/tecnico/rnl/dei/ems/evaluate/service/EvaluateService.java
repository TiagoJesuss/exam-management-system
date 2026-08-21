package pt.ulisboa.tecnico.rnl.dei.ems.evaluate.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateState;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateType;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.dto.EvaluateInboxDto;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.repository.EvaluateRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam.ExamState;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;

@Service
@Transactional
public class EvaluateService {

	@Autowired
	private EvaluateRepository evaluateRepository;

	@Autowired
	private QuestionRepository questionRepository;

	@Transactional(readOnly = true)
	public List<EvaluateInboxDto> getMyInbox() {
		Person current = requireCurrentPerson();
		return evaluateRepository
				.findByProfessor_IdAndStateIn(
						current.getId(),
						EnumSet.of(EvaluateState.PENDING, EvaluateState.IN_PROGRESS))
				.stream()
				.map(EvaluateInboxDto::new)
				.toList();
	}

	public EvaluateInboxDto getMineAndStart(long id) {
		Evaluate evaluate = fetchOwnedOrThrow(id);
		if (evaluate.getState() == EvaluateState.PENDING) {
			evaluate.setState(EvaluateState.IN_PROGRESS);
		}
		return new EvaluateInboxDto(evaluate);
	}

	@Transactional(readOnly = true)
	public Path getOwnedImagePath(long id) {
		Evaluate evaluate = fetchOwnedOrThrow(id);
		String imagePath = evaluate.getQuestion().getPath();
		if (imagePath == null || imagePath.isBlank()) {
			throw new DEIException(ErrorMessage.QUESTION_FILE_MISSING);
		}
		Path path = Paths.get(imagePath);
		if (!Files.isRegularFile(path)) {
			throw new DEIException(ErrorMessage.QUESTION_FILE_MISSING);
		}
		return path;
	}

	public EvaluateInboxDto submitGrade(long id, Double grade) {
		Evaluate evaluate = fetchOwnedOrThrow(id);

		if (evaluate.getState() != EvaluateState.PENDING
				&& evaluate.getState() != EvaluateState.IN_PROGRESS) {
			throw new DEIException(ErrorMessage.EVALUATE_NOT_GRADABLE);
		}

		double maxScore = evaluate.getQuestion().getMaxScore();
		if (grade == null || grade < 0 || grade > maxScore) {
			throw new DEIException(ErrorMessage.EVALUATE_GRADE_INVALID, String.valueOf(maxScore));
		}

		evaluate.setGrade(grade);
		evaluate.setState(EvaluateState.GRADED);
		tryCompleteExam(evaluate.getQuestion().getExam());
		return new EvaluateInboxDto(evaluate);
	}

	private void tryCompleteExam(Exam exam) {
		long examId = exam.getId();
		long questionCount = questionRepository.countByExam_Id(examId);
		List<Evaluate> evaluates = evaluateRepository.findByQuestion_Exam_IdAndType(
				examId, EvaluateType.INITIAL);

		if (questionCount == 0 || evaluates.size() != questionCount) {
			return;
		}
		if (evaluates.stream().anyMatch(e -> e.getState() != EvaluateState.GRADED)) {
			return;
		}

		double total = evaluates.stream()
				.mapToDouble(e -> e.getGrade() != null ? e.getGrade() : 0.0)
				.sum();
		exam.setFinalScore(total);
		exam.setState(ExamState.GRADED);
	}

	private Evaluate fetchOwnedOrThrow(long id) {
		Person current = requireCurrentPerson();
		Evaluate evaluate = evaluateRepository.findById(id)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_EVALUATE, String.valueOf(id)));
		if (!evaluate.getProfessor().getId().equals(current.getId())) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		return evaluate;
	}

	private Person requireCurrentPerson() {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}
		return current;
	}
}
