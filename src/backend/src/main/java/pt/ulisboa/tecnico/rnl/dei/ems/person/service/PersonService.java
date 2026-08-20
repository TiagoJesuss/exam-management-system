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

	@Transactional
	public List<PersonDto> getPeople() {
		return personRepository.findAll().stream()
				.map(PersonDto::new)
				.toList();
	}

	@Transactional
	public PersonDto createPerson(PersonDto personDto) {
		if (personRepository.existsByEmail(personDto.email())) {
			throw new DEIException(ErrorMessage.PERSON_ALREADY_EXISTS, personDto.email());
		}

		Person person = new Person(
				personDto.name(),
				personDto.email(),
				passwordEncoder.encode(personDto.password()),
				PersonType.valueOf(personDto.type().toUpperCase()));

		applySchoolAndSubject(person, personDto);

		return new PersonDto(personRepository.save(person));
	}

	@Transactional
	public PersonDto getPerson(long id) {
		return new PersonDto(fetchPersonOrThrow(id));
	}

	@Transactional
	public PersonDto updatePerson(long id, PersonDto personDto) {
		Person person = fetchPersonOrThrow(id);

		person.setName(personDto.name());
		person.setType(PersonType.valueOf(personDto.type().toUpperCase()));
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
