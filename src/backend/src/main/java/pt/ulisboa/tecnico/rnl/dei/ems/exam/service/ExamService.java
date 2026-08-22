package pt.ulisboa.tecnico.rnl.dei.ems.exam.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateType;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.repository.EvaluateRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam.ExamState;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.CloseReviewWindowsResultDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.DistributionResultDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.ExamConsultationDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.ExamDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.FragmentGradeDto;
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
import pt.ulisboa.tecnico.rnl.dei.ems.school.domain.School;
import pt.ulisboa.tecnico.rnl.dei.ems.school.repository.SchoolRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.repository.SubjectRepository;

// Service class for managing Exam entities
@Service
@Transactional
public class ExamService {

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private SchoolRepository schoolRepository;

	@Autowired
	private QuestionRepository questionRepository;

	@Autowired
	private EvaluateRepository evaluateRepository;

	@Autowired
	private ReviewRequestRepository reviewRequestRepository;

	@Value("${ems.uploads.dir:uploads}")
	private String uploadsDir;

	@Value("${ems.review.deadline-hours:48}")
	private long reviewDeadlineHours;

	private Exam fetchExamOrThrow(long id) {
		return examRepository.findById(id)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_EXAM, Long.toString(id)));
	}

	@Transactional
	public List<ExamDto> getExams() {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}

		return examRepository.findAll().stream()
				.filter(exam -> canAccessExam(current, exam))
				.map(ExamDto::new)
				.toList();
	}

	@Transactional
	public ExamDto getExam(long id) {
		Exam exam = fetchExamOrThrow(id);
		assertCanAccessExam(exam);
		return new ExamDto(exam);
	}

	@Transactional(readOnly = true)
	public List<ExamDto> getMyConsultableExams() {
		Person current = requireCurrentPerson();
		return examRepository
				.findByStudent_IdAndStateIn(
						current.getId(),
						EnumSet.of(
								ExamState.GRADES_PUBLISHED,
								ExamState.RELEASED,
								ExamState.REVIEW_CLOSED,
								ExamState.REVIEW_GRADING,
								ExamState.FINAL))
				.stream()
				.map(ExamDto::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public ExamConsultationDto getConsultation(long id) {
		Exam exam = fetchOwnedConsultableExam(id);
		List<Evaluate> initials = evaluateRepository.findByQuestion_Exam_IdAndType(
				exam.getId(), EvaluateType.INITIAL);
		var gradeByQuestionId = initials.stream()
				.collect(Collectors.toMap(e -> e.getQuestion().getId(), Evaluate::getGrade, (a, b) -> a));

		evaluateRepository.findByQuestion_Exam_IdAndType(exam.getId(), EvaluateType.REVIEW)
				.stream()
				.filter(e -> e.getGrade() != null)
				.forEach(e -> gradeByQuestionId.put(e.getQuestion().getId(), e.getGrade()));

		List<FragmentGradeDto> fragments = questionRepository.findByExam_IdOrderByNumberAsc(exam.getId())
				.stream()
				.map(q -> new FragmentGradeDto(
						q.getNumber(),
						q.getMaxScore(),
						gradeByQuestionId.get(q.getId())))
				.toList();

		return new ExamConsultationDto(
				exam.getId(),
				exam.getState().toString(),
				exam.getSubject().getCode(),
				exam.getFinalScore(),
				exam.getReviewDeadline(),
				exam.isConsultationRequested(),
				fragments);
	}

	@Transactional
	public ExamDto requestConsultation(long id) {
		Person current = requireCurrentPerson();
		Exam exam = fetchExamOrThrow(id);
		if (!exam.getStudent().getId().equals(current.getId())) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		if (exam.getState() != ExamState.GRADES_PUBLISHED) {
			throw new DEIException(ErrorMessage.EXAM_CONSULTATION_REQUEST_INVALID);
		}
		exam.setConsultationRequested(true);
		return new ExamDto(exam);
	}

	@Transactional
	public CloseReviewWindowsResultDto closeExpiredReviewWindows() {
		return closeReviewWindows(false);
	}

	@Transactional
	public CloseReviewWindowsResultDto forceCloseReviewWindows() {
		return closeReviewWindows(true);
	}

	private CloseReviewWindowsResultDto closeReviewWindows(boolean force) {
		Instant now = Instant.now();
		EnumSet<ExamState> openStates = EnumSet.of(ExamState.GRADES_PUBLISHED, ExamState.RELEASED);

		int closedToFinal = 0;
		int closedToReviewClosed = 0;
		List<Exam> forReviewDistribution = new ArrayList<>();

		for (Exam exam : examRepository.findAll()) {
			if (!openStates.contains(exam.getState())) {
				continue;
			}
			if (!force && (exam.getReviewDeadline() == null || exam.getReviewDeadline().isAfter(now))) {
				continue;
			}

			if (applyReviewWindowClose(exam)) {
				closedToReviewClosed++;
				forReviewDistribution.add(exam);
			} else {
				closedToFinal++;
			}
		}

		if (!forReviewDistribution.isEmpty()) {
			distributeReviewAssignments(forReviewDistribution);
		}

		return new CloseReviewWindowsResultDto(closedToFinal, closedToReviewClosed);
	}

	/** @return true if closed to REVIEW_CLOSED, false if closed to FINAL */
	private boolean applyReviewWindowClose(Exam exam) {
		boolean hasRequests = reviewRequestRepository.existsByQuestion_Exam_Id(exam.getId());
		if (hasRequests) {
			exam.setState(ExamState.REVIEW_CLOSED);
			return true;
		}
		exam.setState(ExamState.FINAL);
		return false;
	}

	private void distributeReviewAssignments(List<Exam> exams) {
		List<Long> examIds = exams.stream().map(Exam::getId).toList();
		List<ReviewRequest> pending = reviewRequestRepository
				.findByQuestion_Exam_IdInAndStatus(examIds, ReviewRequestStatus.PENDING);
		if (pending.isEmpty()) {
			for (Exam exam : exams) {
				exam.setState(ExamState.REVIEW_GRADING);
			}
			return;
		}

		List<Person> allTeachers = personRepository.findAll().stream()
				.filter(p -> p.getType() == PersonType.TEACHER && p.getSubject() != null)
				.toList();

		Map<Long, Integer> loadByTeacher = new HashMap<>();
		Map<Long, Exam> examsById = exams.stream()
				.collect(Collectors.toMap(Exam::getId, e -> e, (a, b) -> a));

		for (ReviewRequest request : pending) {
			Question question = request.getQuestion();
			if (evaluateRepository.existsByQuestion_IdAndType(question.getId(), EvaluateType.REVIEW)) {
				request.setStatus(ReviewRequestStatus.DISTRIBUTED);
				continue;
			}

			Long subjectId = question.getExam().getSubject().getId();
			List<Person> teachers = allTeachers.stream()
					.filter(t -> t.getSubject().getId().equals(subjectId))
					.toList();
			if (teachers.isEmpty()) {
				throw new DEIException(
						ErrorMessage.NO_TEACHERS_FOR_SUBJECT,
						question.getExam().getSubject().getCode());
			}

			Long initialProfessorId = evaluateRepository
					.findByQuestion_IdAndType(question.getId(), EvaluateType.INITIAL)
					.map(e -> e.getProfessor().getId())
					.orElse(null);

			Person student = question.getExam().getStudent();
			List<Person> candidates = pickReviewCandidates(teachers, student, initialProfessorId);
			Person chosen = pickLeastLoaded(candidates, loadByTeacher);
			evaluateRepository.save(new Evaluate(chosen, question, EvaluateType.REVIEW));
			loadByTeacher.merge(chosen.getId(), 1, Integer::sum);
			request.setStatus(ReviewRequestStatus.DISTRIBUTED);
		}

		for (Exam exam : examsById.values()) {
			exam.setState(ExamState.REVIEW_GRADING);
		}
	}

	private List<Person> pickReviewCandidates(
			List<Person> teachers, Person student, Long initialProfessorId) {
		List<Person> notInitial = teachers.stream()
				.filter(t -> initialProfessorId == null || !t.getId().equals(initialProfessorId))
				.toList();
		List<Person> pool = notInitial.isEmpty() ? teachers : notInitial;

		List<Person> preferred = new ArrayList<>();
		for (Person teacher : pool) {
			if (isPreferredTeacher(teacher, student)) {
				preferred.add(teacher);
			}
		}
		return preferred.isEmpty() ? pool : preferred;
	}

	@Transactional
	public List<ExamDto> publishGrades(String schoolCode, String subjectCode) {
		Person current = requireCurrentPerson();
		School schoolFilter = resolveSchoolForPublish(current, schoolCode);

		Subject subjectFilter = null;
		if (subjectCode != null && !subjectCode.isBlank()) {
			subjectFilter = subjectRepository.findByCode(subjectCode)
					.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SUBJECT, subjectCode));
		}

		EnumSet<ExamState> readyStates = EnumSet.of(
				ExamState.GRADED, ExamState.GRADES_PUBLISHED, ExamState.RELEASED);

		Subject subject = subjectFilter;
		School school = schoolFilter;
		List<Exam> scoped = examRepository.findAll().stream()
				.filter(exam -> subject == null || exam.getSubject().getId().equals(subject.getId()))
				.filter(exam -> school == null
						|| (exam.getStudent().getSchool() != null
								&& exam.getStudent().getSchool().getId().equals(school.getId())))
				.filter(exam -> canAccessExam(current, exam))
				.toList();

		boolean notAllReady = scoped.stream().anyMatch(exam -> !readyStates.contains(exam.getState()));
		if (notAllReady) {
			throw new DEIException(ErrorMessage.EXAM_PUBLISH_NOT_ALL_GRADED);
		}

		return scoped.stream()
				.filter(exam -> exam.getState() == ExamState.GRADED)
				.peek(this::applyPublishGrades)
				.map(ExamDto::new)
				.toList();
	}

	@Transactional
	public ExamDto releaseExam(long id) {
		Exam exam = fetchExamOrThrow(id);
		assertCanAccessExam(exam);

		if (exam.getState() != ExamState.GRADES_PUBLISHED) {
			throw new DEIException(ErrorMessage.EXAM_NOT_PUBLISHED_FOR_RELEASE);
		}

		applyPdfRelease(exam);
		return new ExamDto(exam);
	}

	@Transactional
	public List<ExamDto> releaseExams(String schoolCode, String subjectCode) {
		Person current = requireCurrentPerson();
		if (schoolCode == null || schoolCode.isBlank() || subjectCode == null || subjectCode.isBlank()) {
			throw new DEIException(ErrorMessage.EXAM_RELEASE_FILTERS_REQUIRED);
		}

		School school = schoolRepository.findByCode(schoolCode)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SCHOOL, schoolCode));
		Subject subject = subjectRepository.findByCode(subjectCode)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SUBJECT, subjectCode));

		if (current.getType() != PersonType.ADMINISTRATOR) {
			if (current.getSchool() == null
					|| !current.getSchool().getId().equals(school.getId())) {
				throw new DEIException(ErrorMessage.ACCESS_DENIED);
			}
		}

		return examRepository.findAll().stream()
				.filter(exam -> exam.getState() == ExamState.GRADES_PUBLISHED)
				.filter(exam -> exam.getSubject().getId().equals(subject.getId()))
				.filter(exam -> exam.getStudent().getSchool() != null
						&& exam.getStudent().getSchool().getId().equals(school.getId()))
				.filter(exam -> canAccessExam(current, exam))
				.peek(this::applyPdfRelease)
				.map(ExamDto::new)
				.toList();
	}

	/** Null school filter means no school restriction (admin, no filter). Staff always scoped to own school. */
	private School resolveSchoolForPublish(Person current, String schoolCode) {
		if (current.getType() == PersonType.ADMINISTRATOR) {
			if (schoolCode == null || schoolCode.isBlank()) {
				return null;
			}
			return schoolRepository.findByCode(schoolCode)
					.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SCHOOL, schoolCode));
		}
		if (current.getSchool() == null) {
			throw new DEIException(ErrorMessage.SCHOOL_STAFF_MUST_HAVE_SCHOOL, null);
		}
		if (schoolCode != null && !schoolCode.isBlank()
				&& !current.getSchool().getCode().equals(schoolCode)) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		return current.getSchool();
	}

	private void applyPublishGrades(Exam exam) {
		exam.setState(ExamState.GRADES_PUBLISHED);
		exam.setReviewDeadline(Instant.now().plus(reviewDeadlineHours, ChronoUnit.HOURS));
	}

	private void applyPdfRelease(Exam exam) {
		exam.setState(ExamState.RELEASED);
	}

	@Transactional
	public List<ExamDto> lockSegmentedExams() {
		List<Exam> locked = examRepository.findAll().stream()
				.filter(exam -> exam.getState() == ExamState.SEGMENTED)
				.peek(exam -> exam.setState(ExamState.LOCKED))
				.toList();
		return examRepository.saveAll(locked).stream()
				.map(ExamDto::new)
				.toList();
	}

	@Transactional
	public DistributionResultDto distributeLockedExams() {
		boolean pending = examRepository.findAll().stream()
				.anyMatch(exam -> exam.getState() == ExamState.UPLOADED
						|| exam.getState() == ExamState.SEGMENTED);
		if (pending) {
			throw new DEIException(ErrorMessage.EXAM_NOT_ALL_LOCKED);
		}

		List<Exam> lockedExams = examRepository.findAll().stream()
				.filter(exam -> exam.getState() == ExamState.LOCKED)
				.toList();
		if (lockedExams.isEmpty()) {
			return new DistributionResultDto(0, 0);
		}

		for (Exam exam : lockedExams) {
			if (evaluateRepository.existsByQuestion_Exam_IdAndType(exam.getId(), EvaluateType.INITIAL)) {
				throw new DEIException(ErrorMessage.EXAM_ALREADY_DISTRIBUTED);
			}
		}

		Map<Long, List<Question>> questionsBySubject = questionRepository.findAll().stream()
				.filter(q -> q.getExam().getState() == ExamState.LOCKED)
				.sorted(Comparator.comparing((Question q) -> q.getExam().getId()).thenComparingInt(Question::getNumber))
				.collect(Collectors.groupingBy(q -> q.getExam().getSubject().getId()));

		List<Person> allTeachers = personRepository.findAll().stream()
				.filter(p -> p.getType() == PersonType.TEACHER && p.getSubject() != null)
				.toList();

		int assignedCount = 0;
		int forcedSameSchoolCount = 0;
		Map<Long, Integer> loadByTeacher = new HashMap<>();

		for (Map.Entry<Long, List<Question>> entry : questionsBySubject.entrySet()) {
			Long subjectId = entry.getKey();
			List<Question> questions = entry.getValue();
			List<Person> teachers = allTeachers.stream()
					.filter(t -> t.getSubject().getId().equals(subjectId))
					.toList();
			if (teachers.isEmpty()) {
				String code = questions.get(0).getExam().getSubject().getCode();
				throw new DEIException(ErrorMessage.NO_TEACHERS_FOR_SUBJECT, code);
			}

			for (Question question : questions) {
				Person student = question.getExam().getStudent();
				List<Person> preferred = new ArrayList<>();
				for (Person teacher : teachers) {
					if (isPreferredTeacher(teacher, student)) {
						preferred.add(teacher);
					}
				}
				boolean forced = preferred.isEmpty();
				List<Person> candidates = forced ? teachers : preferred;
				Person chosen = pickLeastLoaded(candidates, loadByTeacher);
				evaluateRepository.save(new Evaluate(chosen, question, EvaluateType.INITIAL));
				loadByTeacher.merge(chosen.getId(), 1, Integer::sum);
				assignedCount++;
				if (forced) {
					forcedSameSchoolCount++;
				}
			}
		}

		for (Exam exam : lockedExams) {
			exam.setState(ExamState.IN_GRADING);
		}
		examRepository.saveAll(lockedExams);

		return new DistributionResultDto(assignedCount, forcedSameSchoolCount);
	}

	private boolean isPreferredTeacher(Person teacher, Person student) {
		if (teacher.getSchool() == null || student.getSchool() == null) {
			return true;
		}
		return !teacher.getSchool().getId().equals(student.getSchool().getId());
	}

	private Person pickLeastLoaded(List<Person> candidates, Map<Long, Integer> loadByTeacher) {
		return candidates.stream()
				.min(Comparator
						.comparingInt((Person t) -> loadByTeacher.getOrDefault(t.getId(), 0))
						.thenComparing(Person::getId))
				.orElseThrow();
	}

	@Transactional(readOnly = true)
	public Path getExamFilePath(long id) {
		Exam exam = fetchExamOrThrow(id);
		Person current = requireCurrentPerson();
		if (!canAccessExam(current, exam) && !canViewPdf(current, exam)) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}

		if (exam.getPath() == null || exam.getPath().isBlank()) {
			throw new DEIException(ErrorMessage.EXAM_FILE_MISSING);
		}
		Path path = Paths.get(exam.getPath());
		if (!Files.isRegularFile(path)) {
			throw new DEIException(ErrorMessage.EXAM_FILE_MISSING);
		}
		return path;
	}

	private Exam fetchOwnedConsultableExam(long id) {
		Person current = requireCurrentPerson();
		Exam exam = fetchExamOrThrow(id);
		if (!canConsultGrades(current, exam)) {
			if (exam.getStudent().getId().equals(current.getId())) {
				throw new DEIException(ErrorMessage.EXAM_NOT_OPEN_FOR_CONSULTATION);
			}
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		return exam;
	}

	private boolean canConsultGrades(Person current, Exam exam) {
		return current.getType() == PersonType.STUDENT
				&& exam.getStudent().getId().equals(current.getId())
				&& (exam.getState() == ExamState.GRADES_PUBLISHED
						|| exam.getState() == ExamState.RELEASED
						|| exam.getState() == ExamState.REVIEW_CLOSED
						|| exam.getState() == ExamState.REVIEW_GRADING
						|| exam.getState() == ExamState.FINAL);
	}

	private boolean canViewPdf(Person current, Exam exam) {
		return current.getType() == PersonType.STUDENT
				&& exam.getStudent().getId().equals(current.getId())
				&& (exam.getState() == ExamState.RELEASED
						|| exam.getState() == ExamState.REVIEW_CLOSED
						|| exam.getState() == ExamState.REVIEW_GRADING
						|| exam.getState() == ExamState.FINAL);
	}

	private Person requireCurrentPerson() {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}
		return current;
	}

	private void assertCanAccessExam(Exam exam) {
		Person current = requireCurrentPerson();
		if (!canAccessExam(current, exam)) {
			throw new DEIException(ErrorMessage.EXAM_STUDENT_WRONG_SCHOOL);
		}
	}

	private boolean canAccessExam(Person current, Exam exam) {
		if (current.getType() == PersonType.ADMINISTRATOR) {
			return true;
		}
		if (current.getType() == PersonType.STUDENT) {
			return false;
		}
		return current.getSchool() != null
				&& exam.getStudent().getSchool() != null
				&& current.getSchool().getId().equals(exam.getStudent().getSchool().getId());
	}

	@Transactional
	public ExamDto uploadExam(MultipartFile file, long studentId, String subjectCode) {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}

		if (areSubmissionsLocked()) {
			throw new DEIException(ErrorMessage.EXAM_SUBMISSIONS_LOCKED);
		}

		Person student = personRepository.findById(studentId)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_PERSON, Long.toString(studentId)));
		if (student.getType() != PersonType.STUDENT) {
			throw new DEIException(ErrorMessage.NOT_STUDENT, Long.toString(studentId));
		}
		if (student.getSchool() == null) {
			throw new DEIException(ErrorMessage.STUDENT_MUST_HAVE_SCHOOL, null);
		}

		if (current.getType() != PersonType.ADMINISTRATOR) {
			if (current.getSchool() == null) {
				throw new DEIException(ErrorMessage.SCHOOL_STAFF_MUST_HAVE_SCHOOL, null);
			}
			if (!current.getSchool().getId().equals(student.getSchool().getId())) {
				throw new DEIException(ErrorMessage.EXAM_STUDENT_WRONG_SCHOOL);
			}
		}

		Subject subject = subjectRepository.findByCode(subjectCode)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SUBJECT, subjectCode));

		if (examRepository.existsByStudent_IdAndSubject_Id(student.getId(), subject.getId())) {
			throw new DEIException(ErrorMessage.EXAM_ALREADY_EXISTS_FOR_STUDENT_SUBJECT, subjectCode);
		}

		String path = storePdf(file, studentId, subjectCode);
		Exam exam = new Exam(student, subject, path);
		return new ExamDto(examRepository.save(exam));
	}

	@Transactional
	public ExamDto segmentExam(long id, MultipartFile[] files, Double[] maxScores) {
		Exam exam = fetchExamOrThrow(id);
		assertCanAccessExam(exam);

		if (exam.getState() != ExamState.UPLOADED) {
			throw new DEIException(ErrorMessage.EXAM_NOT_UPLOADED);
		}
		if (files == null || maxScores == null || files.length == 0 || files.length != maxScores.length) {
			throw new DEIException(ErrorMessage.EXAM_SEGMENT_INVALID);
		}

		for (int i = 0; i < files.length; i++) {
			MultipartFile file = files[i];
			Double maxScore = maxScores[i];
			if (file == null || file.isEmpty() || maxScore == null || maxScore <= 0) {
				throw new DEIException(ErrorMessage.EXAM_SEGMENT_INVALID);
			}
			int number = i + 1;
			String path = storeQuestionImage(file, exam.getId(), number);
			questionRepository.save(new Question(exam, number, maxScore, path));
		}

		boolean submissionsClosed = areSubmissionsLocked();
		exam.setState(submissionsClosed ? ExamState.LOCKED : ExamState.SEGMENTED);
		return new ExamDto(examRepository.save(exam));
	}


	private boolean areSubmissionsLocked() {
		EnumSet<ExamState> midCycle = EnumSet.of(
				ExamState.LOCKED,
				ExamState.IN_GRADING,
				ExamState.GRADED,
				ExamState.GRADES_PUBLISHED,
				ExamState.RELEASED,
				ExamState.REVIEW_CLOSED,
				ExamState.REVIEW_GRADING);
		return examRepository.findAll().stream()
				.anyMatch(exam -> midCycle.contains(exam.getState()));
	}

	private String storePdf(MultipartFile file, long studentId, String subjectCode) {
		if (file == null || file.isEmpty()) {
			throw new DEIException(ErrorMessage.EXAM_FILE_EMPTY);
		}

		String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
		String contentType = file.getContentType() != null ? file.getContentType() : "";
		boolean isPdf = originalName.toLowerCase().endsWith(".pdf")
				|| contentType.equalsIgnoreCase("application/pdf");
		if (!isPdf) {
			throw new DEIException(ErrorMessage.EXAM_NOT_PDF);
		}

		try {
			String filename = studentId + "_" + subjectCode + "_" + UUID.randomUUID() + ".pdf";
			Path destination = Paths.get(uploadsDir, "exams", filename).toAbsolutePath().normalize();
			Files.createDirectories(destination.getParent());
			Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
			return destination.toString();
		} catch (IOException e) {
			throw new DEIException(ErrorMessage.EXAM_STORAGE_FAILED);
		}
	}

	private String storeQuestionImage(MultipartFile file, long examId, int number) {
		try {
			String filename = examId + "_q" + number + "_" + UUID.randomUUID() + ".png";
			Path destination = Paths.get(uploadsDir, "questions", filename).toAbsolutePath().normalize();
			Files.createDirectories(destination.getParent());
			Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
			return destination.toString();
		} catch (IOException e) {
			throw new DEIException(ErrorMessage.QUESTION_STORAGE_FAILED);
		}
	}
}
