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

/**
 * Seeds one demo account per role on startup (only if the email is missing),
 * so you can log in immediately and test each permission set.
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
		seedPerson("Admin User", "admin@dei.tecnico.ulisboa.pt", "admin123", PersonType.ADMINISTRATOR);
		seedPerson("Staff User", "staff@dei.tecnico.ulisboa.pt", "staff123", PersonType.SCHOOL_STAFF);
		seedPerson("Teacher User", "teacher@dei.tecnico.ulisboa.pt", "teacher123", PersonType.TEACHER);
		seedPerson("Student User", "student@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT);
		seedSchool("Escola Secundária Luis de Freitas Branco", "Oeiras", "ESLFB");
		seedSchool("Escola Secundária Rainha Dona Leonor", "Lisboa", "ESRDL");
		seedSubject("Matemática A", "MAT-A");
		seedSubject("Física e Química A", "FQ-A");
		seedSubject("Português", "POR");
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
		if (personRepository.existsByEmail(email)) {
			return;
		}
		personRepository.save(new Person(name, email, passwordEncoder.encode(rawPassword), type));
		logger.info("Seeded demo account: {} ({})", email, type);
	}
}
