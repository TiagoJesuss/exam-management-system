package pt.ulisboa.tecnico.rnl.dei.ems.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;
import pt.ulisboa.tecnico.rnl.dei.ems.person.repository.PersonRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.school.repository.SchoolRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.repository.SubjectRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.school.domain.School;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;

/**
 * Seeds demo data on startup (only if missing), so you can log in and validate the platform.
 *
 * DEV CONVENIENCE ONLY
 *
 *   admin@dei.tecnico.ulisboa.pt   / admin123     (ADMINISTRATOR)
 *   staff@dei.tecnico.ulisboa.pt   / staff123     (SCHOOL_STAFF)
 *   teacher@dei.tecnico.ulisboa.pt / teacher123   (TEACHER)
 *   student@dei.tecnico.ulisboa.pt / student123   (STUDENT)
 */
@Component
public class DataInitializer implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

	private final PersonRepository personRepository;
	private final PasswordEncoder passwordEncoder;
	private final SchoolRepository schoolRepository;
	private final SubjectRepository subjectRepository;

	public DataInitializer(PersonRepository personRepository, PasswordEncoder passwordEncoder, SchoolRepository schoolRepository, SubjectRepository subjectRepository) {
		this.personRepository = personRepository;
		this.passwordEncoder = passwordEncoder;
		this.schoolRepository = schoolRepository;
		this.subjectRepository = subjectRepository;
	}

	@Override
	public void run(String... args) {
		seedSchool("Escola Secundária Luis de Freitas Branco", "Oeiras", "ESLFB");
		seedSchool("Escola Secundária Rainha Dona Leonor", "Lisboa", "ESRDL");

		seedSubject("Português", "POR");
		seedSubject("Matemática A", "MAT-A");
		seedSubject("Física e Química A", "FQ-A");
		seedSubject("Biologia e Geologia", "BG");

		seedPerson("Administrador Sistema", "admin@dei.tecnico.ulisboa.pt", "admin123", PersonType.ADMINISTRATOR);
		seedPerson("Funcionário Escola", "staff@dei.tecnico.ulisboa.pt", "staff123", PersonType.SCHOOL_STAFF, "ESLFB");

		seedPerson("Ana Ferreira", "teacher@dei.tecnico.ulisboa.pt", "teacher123", PersonType.TEACHER, "ESRDL", "FQ-A");
		seedPerson("Carlos Mendes", "carlos.mendes@dei.tecnico.ulisboa.pt", "teacher123", PersonType.TEACHER, "ESLFB", "MAT-A");
		seedPerson("Sofia Ribeiro", "sofia.ribeiro@dei.tecnico.ulisboa.pt", "teacher123", PersonType.TEACHER, "ESLFB", "BG");

		seedPerson("João Silva", "student@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESLFB");
		seedPerson("Maria Costa", "aluno2@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESLFB");
		seedPerson("Pedro Santos", "aluno3@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESLFB");
		seedPerson("Inês Oliveira", "aluno4@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESLFB");
		seedPerson("Tiago Pereira", "aluno5@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESLFB");
		seedPerson("Beatriz Almeida", "aluno6@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESRDL");
		seedPerson("Ricardo Lopes", "aluno7@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESRDL");
		seedPerson("Catarina Sousa", "aluno8@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESRDL");
		seedPerson("Miguel Rocha", "aluno9@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESRDL");
		seedPerson("Ana Martins", "aluno10@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT, "ESRDL");
	}

	private void seedSchool(String name, String region, String code) {
		if (schoolRepository.existsByCode(code)) {
			return;
		}
		schoolRepository.save(new School(name, region, code));
		logger.info("Seeded demo school: {}", name);
	}

	private void seedSubject(String name, String code) {
		if (subjectRepository.existsByCode(code)) {
			return;
		}
		subjectRepository.save(new Subject(name, code));
		logger.info("Seeded demo subject: {}", name);
	}

	private void seedPerson(String name, String email, String rawPassword, PersonType type) {
		seedPerson(name, email, rawPassword, type, null);
	}

	private void seedPerson(String name, String email, String rawPassword, PersonType type, String schoolCode) {
		seedPerson(name, email, rawPassword, type, schoolCode, null);
	}

	private void seedPerson(String name, String email, String rawPassword, PersonType type, String schoolCode, String subjectCode) {
		if (personRepository.existsByEmail(email)) {
			return;
		}
		Person person = new Person(name, email, passwordEncoder.encode(rawPassword), type);
		if (type != PersonType.ADMINISTRATOR) {
			if (schoolCode == null || schoolCode.isEmpty()) {
				switch (type) {
					case STUDENT:
						throw new DEIException(ErrorMessage.STUDENT_MUST_HAVE_SCHOOL, null);
					case SCHOOL_STAFF:
						throw new DEIException(ErrorMessage.SCHOOL_STAFF_MUST_HAVE_SCHOOL, null);
					case TEACHER:
						throw new DEIException(ErrorMessage.TEACHER_MUST_HAVE_SCHOOL, null);
				}
			}
			School school = schoolRepository.findByCode(schoolCode).orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SCHOOL, schoolCode));
			person.setSchool(school);
		}
		if (type == PersonType.TEACHER) {
			if (subjectCode == null || subjectCode.isEmpty()) {
				throw new DEIException(ErrorMessage.TEACHER_MUST_HAVE_SUBJECT, null);
			}
			Subject subject = subjectRepository.findByCode(subjectCode).orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SUBJECT, subjectCode));
			person.setSubject(subject);
		}
		personRepository.save(person);
		logger.info("Seeded demo account: {} ({})", email, type);
	}
}
