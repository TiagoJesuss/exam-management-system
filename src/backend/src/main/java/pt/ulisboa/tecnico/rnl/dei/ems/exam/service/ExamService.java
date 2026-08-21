package pt.ulisboa.tecnico.rnl.dei.ems.exam.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
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
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.DistributionResultDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.ExamDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.repository.ExamRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;
import pt.ulisboa.tecnico.rnl.dei.ems.person.repository.PersonRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;
import pt.ulisboa.tecnico.rnl.dei.ems.question.repository.QuestionRepository;
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
	private QuestionRepository questionRepository;

	@Autowired
	private EvaluateRepository evaluateRepository;

	@Value("${ems.uploads.dir:uploads}")
	private String uploadsDir;

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
		assertCanAccessExam(exam);

		if (exam.getPath() == null || exam.getPath().isBlank()) {
			throw new DEIException(ErrorMessage.EXAM_FILE_MISSING);
		}
		Path path = Paths.get(exam.getPath());
		if (!Files.isRegularFile(path)) {
			throw new DEIException(ErrorMessage.EXAM_FILE_MISSING);
		}
		return path;
	}

	private void assertCanAccessExam(Exam exam) {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}
		if (!canAccessExam(current, exam)) {
			throw new DEIException(ErrorMessage.EXAM_STUDENT_WRONG_SCHOOL);
		}
	}

	private boolean canAccessExam(Person current, Exam exam) {
		if (current.getType() == PersonType.ADMINISTRATOR) {
			return true;
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

		boolean submissionsLocked = examRepository.findAll().stream()
				.anyMatch(exam -> exam.getState() == ExamState.LOCKED
						|| exam.getState() == ExamState.IN_GRADING);
		if (submissionsLocked) {
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

		boolean submissionsClosed = examRepository.findAll().stream()
				.anyMatch(e -> e.getState() == ExamState.LOCKED || e.getState() == ExamState.IN_GRADING);
		exam.setState(submissionsClosed ? ExamState.LOCKED : ExamState.SEGMENTED);
		return new ExamDto(examRepository.save(exam));
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
