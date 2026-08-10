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

// Service class for managing Person entities
@Service
@Transactional
public class PersonService {

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

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
		// Hmm... maybe we should add some validation here

		Person person = new Person(
				personDto.name(),
				personDto.email(),
				passwordEncoder.encode(personDto.password()),
				PersonType.valueOf(personDto.type().toUpperCase()));
		return new PersonDto(personRepository.save(person));
	}

	@Transactional
	public PersonDto getPerson(long id) {
		return new PersonDto(fetchPersonOrThrow(id));
	}

	@Transactional
	public PersonDto updatePerson(long id, PersonDto personDto) {
		Person person = fetchPersonOrThrow(id);

		// Now that i see, updatePerson and createPerson are almost identical, 
		// maybe we should refactor them to avoid code duplication

		person.setName(personDto.name());
		person.setType(PersonType.valueOf(personDto.type().toUpperCase()));
		person.setEmail(personDto.email());
		person.setPassword(passwordEncoder.encode(personDto.password()));

		return new PersonDto(personRepository.save(person));
	}

	@Transactional
	public void deletePerson(long id) {
		fetchPersonOrThrow(id); // ensure exists

		personRepository.deleteById(id);
	}
}
