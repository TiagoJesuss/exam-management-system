package pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam.ExamState;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.repository.ExamRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;
import pt.ulisboa.tecnico.rnl.dei.ems.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.dto.CreateReviewRequestDto;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.dto.ReviewRequestDto;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.repository.ReviewRequestRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;

@Service
@Transactional
public class ReviewRequestService {

	@Autowired
	private ReviewRequestRepository reviewRequestRepository;

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private QuestionRepository questionRepository;

	@Transactional(readOnly = true)
	public List<ReviewRequestDto> listForExam(long examId) {
		Exam exam = requireOwnedExam(examId);
		return reviewRequestRepository.findByQuestion_Exam_Id(exam.getId()).stream()
				.map(ReviewRequestDto::new)
				.toList();
	}

	public ReviewRequestDto create(long examId, CreateReviewRequestDto request) {
		Exam exam = requireOwnedExam(examId);
		assertCanRequestReview(exam);

		if (request == null || request.questionNumber() == null) {
			throw new DEIException(ErrorMessage.NO_SUCH_QUESTION_ON_EXAM, "null");
		}
		String justification = request.justification() != null ? request.justification().trim() : "";
		if (justification.isEmpty()) {
			throw new DEIException(ErrorMessage.REVIEW_JUSTIFICATION_REQUIRED);
		}

		Question question = questionRepository
				.findByExam_IdAndNumber(exam.getId(), request.questionNumber())
				.orElseThrow(() -> new DEIException(
						ErrorMessage.NO_SUCH_QUESTION_ON_EXAM,
						Integer.toString(request.questionNumber())));

		if (reviewRequestRepository.existsByQuestion_Id(question.getId())) {
			throw new DEIException(ErrorMessage.REVIEW_REQUEST_ALREADY_EXISTS);
		}

		ReviewRequest created = reviewRequestRepository.save(new ReviewRequest(question, justification));
		return new ReviewRequestDto(created);
	}

	private Exam requireOwnedExam(long examId) {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}
		Exam exam = examRepository.findById(examId)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_EXAM, Long.toString(examId)));
		if (!exam.getStudent().getId().equals(current.getId())) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		return exam;
	}

	private void assertCanRequestReview(Exam exam) {
		if (exam.getState() != ExamState.RELEASED
				|| exam.getReviewDeadline() == null
				|| !Instant.now().isBefore(exam.getReviewDeadline())) {
			throw new DEIException(ErrorMessage.REVIEW_REQUEST_NOT_ALLOWED);
		}
	}
}
