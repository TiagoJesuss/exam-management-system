package pt.ulisboa.tecnico.rnl.dei.ems.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;
import pt.ulisboa.tecnico.rnl.dei.ems.person.repository.PersonRepository;

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

	public DataInitializer(PersonRepository personRepository, PasswordEncoder passwordEncoder) {
		this.personRepository = personRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		seed("Admin User", "admin@dei.tecnico.ulisboa.pt", "admin123", PersonType.ADMINISTRATOR);
		seed("Staff User", "staff@dei.tecnico.ulisboa.pt", "staff123", PersonType.SCHOOL_STAFF);
		seed("Teacher User", "teacher@dei.tecnico.ulisboa.pt", "teacher123", PersonType.TEACHER);
		seed("Student User", "student@dei.tecnico.ulisboa.pt", "student123", PersonType.STUDENT);
	}

	private void seed(String name, String email, String rawPassword, PersonType type) {
		if (personRepository.existsByEmail(email)) {
			return;
		}
		personRepository.save(new Person(name, email, passwordEncoder.encode(rawPassword), type));
		logger.info("Seeded demo account: {} ({})", email, type);
	}
}
