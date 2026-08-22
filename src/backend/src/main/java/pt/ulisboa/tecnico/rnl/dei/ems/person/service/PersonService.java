package pt.ulisboa.tecnico.rnl.dei.ems.person.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;
import pt.ulisboa.tecnico.rnl.dei.ems.person.dto.PersonDto;
import pt.ulisboa.tecnico.rnl.dei.ems.person.repository.PersonRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.school.repository.SchoolRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.school.domain.School;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.repository.SubjectRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;

// Service class for managing Person entities
@Service
@Transactional
public class PersonService {

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private SchoolRepository schoolRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	private Person fetchPersonOrThrow(long id) {
		return personRepository.findById(id)
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_PERSON, Long.toString(id)));
	}

	@Transactional(readOnly = true)
	public List<PersonDto> getPeople() {
		Person current = SecurityUtils.currentPerson();
		return personRepository.findAll().stream()
				.filter(person -> canSeePerson(current, person))
				.map(PersonDto::new)
				.toList();
	}

	@Transactional
	public PersonDto createPerson(PersonDto personDto) {
		validatePersonFields(personDto, true);
		assertStaffCanManageStudent(null, personDto);

		if (personRepository.existsByEmail(personDto.email())) {
			throw new DEIException(ErrorMessage.PERSON_ALREADY_EXISTS, personDto.email());
		}

		Person person = new Person(
				personDto.name(),
				personDto.email(),
				passwordEncoder.encode(personDto.password()),
				parsePersonType(personDto.type()));

		applySchoolAndSubject(person, personDto);

		return new PersonDto(personRepository.save(person));
	}

	@Transactional(readOnly = true)
	public PersonDto getPerson(long id) {
		Person person = fetchPersonOrThrow(id);
		Person current = SecurityUtils.currentPerson();
		if (!canSeePerson(current, person)) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		return new PersonDto(person);
	}

	/** Staff and teachers only see people from their own school; admin sees everyone. */
	private boolean canSeePerson(Person current, Person target) {
		if (current == null) {
			return true;
		}
		if (current.getType() != PersonType.SCHOOL_STAFF
				&& current.getType() != PersonType.TEACHER) {
			return true;
		}
		return current.getSchool() != null
				&& target.getSchool() != null
				&& current.getSchool().getId().equals(target.getSchool().getId());
	}

	@Transactional
	public PersonDto updatePerson(long id, PersonDto personDto) {
		Person person = fetchPersonOrThrow(id);

		validatePersonFields(personDto, false);
		assertStaffCanManageStudent(person, personDto);

		person.setName(personDto.name());
		person.setType(parsePersonType(personDto.type()));
		if (personRepository.existsByEmail(personDto.email()) && !person.getEmail().equals(personDto.email())) {
			throw new DEIException(ErrorMessage.PERSON_ALREADY_EXISTS, personDto.email());
		}
		person.setEmail(personDto.email());
		if (personDto.password() != null && !personDto.password().isBlank()) {
			person.setPassword(passwordEncoder.encode(personDto.password()));
		}
		applySchoolAndSubject(person, personDto);

		return new PersonDto(personRepository.save(person));
	}

	private void validatePersonFields(PersonDto personDto, boolean requirePassword) {
		if (personDto.name() == null || personDto.name().isBlank()) {
			throw new DEIException(ErrorMessage.PERSON_NAME_NOT_VALID);
		}
		if (personDto.email() == null || personDto.email().isBlank() || !personDto.email().contains("@")) {
			throw new DEIException(ErrorMessage.PERSON_EMAIL_NOT_VALID);
		}
		if (requirePassword && (personDto.password() == null || personDto.password().isBlank())) {
			throw new DEIException(ErrorMessage.PERSON_PASSWORD_NOT_VALID);
		}
	}

	private PersonType parsePersonType(String type) {
		if (type == null || type.isBlank()) {
			throw new DEIException(ErrorMessage.INVALID_PERSON_TYPE);
		}
		try {
			return PersonType.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new DEIException(ErrorMessage.INVALID_PERSON_TYPE);
		}
	}

	private void assertStaffCanManageStudent(Person existing, PersonDto personDto) {
		Person current = SecurityUtils.currentPerson();
		if (current == null || current.getType() != PersonType.SCHOOL_STAFF) {
			return;
		}
		if (current.getSchool() == null) {
			throw new DEIException(ErrorMessage.SCHOOL_STAFF_MUST_HAVE_SCHOOL, null);
		}

		PersonType targetType = parsePersonType(personDto.type());
		if (targetType != PersonType.STUDENT) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		if (existing != null && existing.getType() != PersonType.STUDENT) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		if (existing != null
				&& (existing.getSchool() == null
						|| !existing.getSchool().getId().equals(current.getSchool().getId()))) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
		if (personDto.schoolCode() == null
				|| personDto.schoolCode().isBlank()
				|| !personDto.schoolCode().equals(current.getSchool().getCode())) {
			throw new DEIException(ErrorMessage.ACCESS_DENIED);
		}
	}

	private void applySchoolAndSubject(Person person, PersonDto personDto) {
		boolean hasSchool = personDto.schoolCode() != null && !personDto.schoolCode().isBlank();

		if (person.getType() == PersonType.ADMINISTRATOR) {
			person.setSchool(null);
		} else if (!hasSchool) {
			if (person.getType() == PersonType.STUDENT) {
				throw new DEIException(ErrorMessage.STUDENT_MUST_HAVE_SCHOOL, personDto.schoolCode());
			}
			if (person.getType() == PersonType.SCHOOL_STAFF) {
				throw new DEIException(ErrorMessage.SCHOOL_STAFF_MUST_HAVE_SCHOOL, personDto.schoolCode());
			}
			// TEACHER: school optional
			person.setSchool(null);
		} else {
			School school = schoolRepository.findByCode(personDto.schoolCode())
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SCHOOL, personDto.schoolCode()));
			person.setSchool(school);
		}

		if (person.getType() != PersonType.TEACHER) {
			person.setSubject(null);
		} else if (personDto.subjectCode() == null || personDto.subjectCode().isBlank()) {
			person.setSubject(null);
		} else {
			Subject subject = subjectRepository.findByCode(personDto.subjectCode())
				.orElseThrow(() -> new DEIException(ErrorMessage.NO_SUCH_SUBJECT, personDto.subjectCode()));
			person.setSubject(subject);
		}
	}

	@Transactional
	public void deletePerson(long id) {
		fetchPersonOrThrow(id); // ensure exists

		personRepository.deleteById(id);
	}
}
