package pt.ulisboa.tecnico.rnl.dei.ems.evaluate.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.audit.domain.AuditAction;
import pt.ulisboa.tecnico.rnl.dei.ems.audit.service.AuditService;
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
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest.ReviewRequestStatus;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.repository.ReviewRequestRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;

@Service
@Transactional
public class EvaluateService {

	@Autowired
	private EvaluateRepository evaluateRepository;

	@Autowired
	private QuestionRepository questionRepository;

	@Autowired
	private ReviewRequestRepository reviewRequestRepository;

	@Autowired
	private AuditService auditService;

	@Transactional(readOnly = true)
	public List<EvaluateInboxDto> getMyInbox() {
		Person current = requireCurrentPerson();
		return evaluateRepository
				.findByProfessor_IdAndStateIn(
						current.getId(),
						EnumSet.of(EvaluateState.PENDING, EvaluateState.IN_PROGRESS))
				.stream()
				.map(this::toDto)
				.toList();
	}

	public EvaluateInboxDto getMineAndStart(long id) {
		Evaluate evaluate = fetchOwnedOrThrow(id);
		if (evaluate.getState() == EvaluateState.PENDING) {
			evaluate.setState(EvaluateState.IN_PROGRESS);
		}
		return toDto(evaluate);
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

		Double oldGrade = evaluate.getGrade();
		Person professor = evaluate.getProfessor();
		long questionId = evaluate.getQuestion().getId();
		long examId = evaluate.getQuestion().getExam().getId();
		int questionNumber = evaluate.getQuestion().getNumber();

		evaluate.setGrade(grade);
		evaluate.setState(EvaluateState.GRADED);
		if (evaluate.getType() == EvaluateType.INITIAL) {
			tryCompleteInitialExam(evaluate.getQuestion().getExam());
		} else if (evaluate.getType() == EvaluateType.REVIEW) {
			tryCompleteReviewExam(evaluate.getQuestion().getExam());
		}

		String oldLabel = oldGrade != null ? oldGrade.toString() : "—";
		String message = String.format(
				"Professor %s alterou a nota do fragmento %d (exame %d) de %s para %s.",
				professor.getName(),
				questionNumber,
				examId,
				oldLabel,
				grade);
		auditService.log(
				AuditAction.GRADE_SUBMITTED,
				message,
				"Evaluate",
				evaluate.getId(),
				"questionId=" + questionId + ";oldGrade=" + oldLabel + ";newGrade=" + grade);

		return toDto(evaluate);
	}

	private void tryCompleteInitialExam(Exam exam) {
		if (exam.getState() != ExamState.IN_GRADING) {
			return;
		}

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

	private void tryCompleteReviewExam(Exam exam) {
		if (exam.getState() != ExamState.REVIEW_GRADING) {
			return;
		}

		long examId = exam.getId();
		List<Evaluate> reviews = evaluateRepository.findByQuestion_Exam_IdAndType(
				examId, EvaluateType.REVIEW);
		if (reviews.isEmpty()) {
			return;
		}
		if (reviews.stream().anyMatch(e -> e.getState() != EvaluateState.GRADED)) {
			return;
		}

		var reviewGradeByQuestion = reviews.stream()
				.collect(Collectors.toMap(
						e -> e.getQuestion().getId(),
						Evaluate::getGrade,
						(a, b) -> a));

		List<Evaluate> initials = evaluateRepository.findByQuestion_Exam_IdAndType(
				examId, EvaluateType.INITIAL);
		var initialGradeByQuestion = initials.stream()
				.collect(Collectors.toMap(
						e -> e.getQuestion().getId(),
						Evaluate::getGrade,
						(a, b) -> a));

		double total = questionRepository.findByExam_IdOrderByNumberAsc(examId).stream()
				.mapToDouble(q -> {
					Double review = reviewGradeByQuestion.get(q.getId());
					if (review != null) {
						return review;
					}
					Double initial = initialGradeByQuestion.get(q.getId());
					return initial != null ? initial : 0.0;
				})
				.sum();

		exam.setFinalScore(total);
		exam.setState(ExamState.FINAL);

		reviewRequestRepository.findByQuestion_Exam_Id(examId)
				.forEach(request -> request.setStatus(ReviewRequestStatus.CLOSED));
	}

	private EvaluateInboxDto toDto(Evaluate evaluate) {
		String justification = null;
		Double previousGrade = null;
		if (evaluate.getType() == EvaluateType.REVIEW) {
			long questionId = evaluate.getQuestion().getId();
			justification = reviewRequestRepository.findByQuestion_Id(questionId)
					.map(r -> r.getJustification())
					.orElse(null);
			previousGrade = evaluateRepository
					.findByQuestion_IdAndType(questionId, EvaluateType.INITIAL)
					.map(Evaluate::getGrade)
					.orElse(null);
		}
		return new EvaluateInboxDto(
				evaluate.getId(),
				evaluate.getState().toString(),
				evaluate.getType().toString(),
				evaluate.getQuestion().getNumber(),
				evaluate.getQuestion().getMaxScore(),
				evaluate.getQuestion().getExam().getSubject().getCode(),
				evaluate.getGrade(),
				justification,
				previousGrade);
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
