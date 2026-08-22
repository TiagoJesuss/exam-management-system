package pt.ulisboa.tecnico.rnl.dei.ems.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateState;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateType;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.repository.EvaluateRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam.ExamState;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.repository.ExamRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;
import pt.ulisboa.tecnico.rnl.dei.ems.person.repository.PersonRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;
import pt.ulisboa.tecnico.rnl.dei.ems.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest.ReviewRequestStatus;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.repository.ReviewRequestRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.repository.SubjectRepository;

/**
 * Seeds one demo exam per workflow state (except REVIEW_CLOSED) for platform validation.
 * Runs after {@link DataInitializer} once schools, subjects and people exist.
 */
@Component
public class ExamDemoSeeder {

	private static final Logger logger = LoggerFactory.getLogger(ExamDemoSeeder.class);

	private static final String SEED_PDF = "seed/demo.pdf";
	private static final String SEED_FRAGMENT = "seed/demo-fragment.png";
	private static final double MAX_SCORE = 10.0;
	private static final int REVIEW_DEADLINE_HOURS = 48;

	private final ExamRepository examRepository;
	private final QuestionRepository questionRepository;
	private final EvaluateRepository evaluateRepository;
	private final ReviewRequestRepository reviewRequestRepository;
	private final PersonRepository personRepository;
	private final SubjectRepository subjectRepository;
	private final String uploadsDir;
	private final boolean seedDemoExams;

	public ExamDemoSeeder(
			ExamRepository examRepository,
			QuestionRepository questionRepository,
			EvaluateRepository evaluateRepository,
			ReviewRequestRepository reviewRequestRepository,
			PersonRepository personRepository,
			SubjectRepository subjectRepository,
			@Value("${ems.uploads.dir:uploads}") String uploadsDir,
			@Value("${ems.seed.demo-exams:true}") boolean seedDemoExams) {
		this.examRepository = examRepository;
		this.questionRepository = questionRepository;
		this.evaluateRepository = evaluateRepository;
		this.reviewRequestRepository = reviewRequestRepository;
		this.personRepository = personRepository;
		this.subjectRepository = subjectRepository;
		this.uploadsDir = uploadsDir;
		this.seedDemoExams = seedDemoExams;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void seedDemoExams() {
		if (!seedDemoExams) {
			logger.info("Demo exam seed disabled (ems.seed.demo-exams=false)");
			return;
		}
		if (examRepository.count() > 0) {
			return;
		}

		seedUploaded();
		seedSegmented();
		seedLocked();
		seedInGrading();
		seedGraded();
		seedGradesPublished();
		seedReleased();
		seedReviewGrading();
		seedFinal();

		logger.info("Seeded {} demo exams (one per state, excluding REVIEW_CLOSED)", examRepository.count());
	}

	private void seedUploaded() {
		saveExam("student@dei.tecnico.ulisboa.pt", "MAT-A", ExamState.UPLOADED, null, null, null);
	}

	private void seedSegmented() {
		saveExam("aluno2@dei.tecnico.ulisboa.pt", "FQ-A", ExamState.SEGMENTED, null, null, null);
	}

	private void seedLocked() {
		saveExam("aluno3@dei.tecnico.ulisboa.pt", "BG", ExamState.LOCKED, null, null, null);
	}

	private void seedInGrading() {
		saveExam("aluno4@dei.tecnico.ulisboa.pt", "MAT-A", ExamState.IN_GRADING,
				EvaluateState.PENDING, null, null);
	}

	private void seedGraded() {
		saveExam("aluno5@dei.tecnico.ulisboa.pt", "FQ-A", ExamState.GRADED,
				EvaluateState.GRADED, new double[] { 7.0, 8.0 }, null);
	}

	private void seedGradesPublished() {
		Instant deadline = Instant.now().plus(REVIEW_DEADLINE_HOURS, ChronoUnit.HOURS);
		saveExam("aluno6@dei.tecnico.ulisboa.pt", "BG", ExamState.GRADES_PUBLISHED,
				EvaluateState.GRADED, new double[] { 7.0, 8.0 }, deadline);
	}

	private void seedReleased() {
		Instant deadline = Instant.now().plus(REVIEW_DEADLINE_HOURS, ChronoUnit.HOURS);
		saveExam("aluno7@dei.tecnico.ulisboa.pt", "MAT-A", ExamState.RELEASED,
				EvaluateState.GRADED, new double[] { 7.0, 8.0 }, deadline);
	}

	private void seedReviewGrading() {
		Exam exam = saveExam("aluno8@dei.tecnico.ulisboa.pt", "FQ-A", ExamState.REVIEW_GRADING,
				EvaluateState.GRADED, new double[] { 7.0, 8.0 }, null);

		Question first = questionRepository.findByExam_IdOrderByNumberAsc(exam.getId()).get(0);
		Person teacher = resolveTeacher("FQ-A");

		ReviewRequest request = new ReviewRequest(first, "Erro na correção da questão 1.");
		request.setStatus(ReviewRequestStatus.DISTRIBUTED);
		reviewRequestRepository.save(request);

		Evaluate review = new Evaluate(teacher, first, EvaluateType.REVIEW);
		review.setState(EvaluateState.PENDING);
		evaluateRepository.save(review);
	}

	private void seedFinal() {
		Exam exam = saveExam("aluno10@dei.tecnico.ulisboa.pt", "BG", ExamState.FINAL,
				EvaluateState.GRADED, new double[] { 7.0, 8.0 }, null);

		List<Question> questions = questionRepository.findByExam_IdOrderByNumberAsc(exam.getId());
		Question first = questions.get(0);
		Person teacher = resolveTeacher("BG");

		ReviewRequest request = new ReviewRequest(first, "Nota demasiado baixa na questão 1.");
		request.setStatus(ReviewRequestStatus.CLOSED);
		reviewRequestRepository.save(request);

		Evaluate review = new Evaluate(teacher, first, EvaluateType.REVIEW);
		review.setState(EvaluateState.GRADED);
		review.setGrade(9.0);
		evaluateRepository.save(review);

		exam.setFinalScore(17.0);
		examRepository.save(exam);
	}

	private Exam saveExam(
			String studentEmail,
			String subjectCode,
			ExamState state,
			EvaluateState evaluateState,
			double[] grades,
			Instant reviewDeadline) {
		Person student = resolveStudent(studentEmail);
		Subject subject = resolveSubject(subjectCode);
		String pdfPath = copyExamPdf(student.getId(), subjectCode);

		Exam exam = new Exam(student, subject, pdfPath);
		exam.setState(state);
		if (reviewDeadline != null) {
			exam.setReviewDeadline(reviewDeadline);
		}
		if (grades != null) {
			exam.setFinalScore(grades[0] + grades[1]);
		}
		exam = examRepository.save(exam);

		if (state == ExamState.UPLOADED) {
			return exam;
		}

		List<Question> questions = addQuestions(exam);

		if (evaluateState == null) {
			return exam;
		}

		Person teacher = resolveTeacher(subjectCode);
		addInitialEvaluates(questions, teacher, evaluateState, grades);
		return exam;
	}

	private List<Question> addQuestions(Exam exam) {
		List<Question> questions = new ArrayList<>();
		for (int number = 1; number <= 2; number++) {
			String path = copyQuestionFragment(exam.getId(), number);
			Question question = questionRepository.save(new Question(exam, number, MAX_SCORE, path));
			questions.add(question);
		}
		return questions;
	}

	private void addInitialEvaluates(
			List<Question> questions,
			Person professor,
			EvaluateState state,
			double[] grades) {
		for (int i = 0; i < questions.size(); i++) {
			Evaluate evaluate = new Evaluate(professor, questions.get(i), EvaluateType.INITIAL);
			evaluate.setState(state);
			if (state == EvaluateState.GRADED && grades != null) {
				evaluate.setGrade(grades[i]);
			}
			evaluateRepository.save(evaluate);
		}
	}

	private Person resolveStudent(String email) {
		Person person = personRepository.findByEmail(email)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_PERSON, email));
		if (person.getType() != PersonType.STUDENT) {
			throw new DEIException(ErrorMessage.NOT_STUDENT, email);
		}
		return person;
	}

	private Subject resolveSubject(String code) {
		return subjectRepository.findByCode(code)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SUBJECT, code));
	}

	private Person resolveTeacher(String subjectCode) {
		return personRepository.findAll().stream()
				.filter(p -> p.getType() == PersonType.TEACHER
						&& p.getSubject() != null
						&& subjectCode.equals(p.getSubject().getCode()))
				.findFirst()
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_TEACHERS_FOR_SUBJECT, subjectCode));
	}

	private String copyExamPdf(long studentId, String subjectCode) {
		String filename = "seed_" + studentId + "_" + subjectCode + "_" + UUID.randomUUID() + ".pdf";
		Path destination = Paths.get(uploadsDir, "exams", filename).toAbsolutePath().normalize();
		return copySeedAsset(SEED_PDF, destination);
	}

	private String copyQuestionFragment(long examId, int number) {
		String filename = "seed_" + examId + "_q" + number + "_" + UUID.randomUUID() + ".png";
		Path destination = Paths.get(uploadsDir, "questions", filename).toAbsolutePath().normalize();
		return copySeedAsset(SEED_FRAGMENT, destination);
	}

	private String copySeedAsset(String classpathResource, Path destination) {
		try {
			Files.createDirectories(destination.getParent());
			ClassPathResource resource = new ClassPathResource(classpathResource);
			try (InputStream in = resource.getInputStream()) {
				Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
			}
			return destination.toString();
		} catch (IOException e) {
			throw new DEIException(ErrorMessage.EXAM_STORAGE_FAILED);
		}
	}
}
