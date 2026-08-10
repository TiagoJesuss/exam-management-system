package pt.ulisboa.tecnico.rnl.dei.ems.person.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;

// Data Transfer Object, to communicate with frontend.
// `password` is WRITE_ONLY: it is accepted when creating/updating a person but
// never serialized back to the client.
public record PersonDto(
		long id,
		String name,
		String email,
		@JsonProperty(access = Access.WRITE_ONLY) String password,
		String type) {
	public PersonDto(Person person) {
		this(person.getId(), person.getName(), person.getEmail(), null, person.getType().toString());
	}
}
