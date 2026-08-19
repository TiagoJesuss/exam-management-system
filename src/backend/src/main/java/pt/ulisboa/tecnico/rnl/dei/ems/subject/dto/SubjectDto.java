package pt.ulisboa.tecnico.rnl.dei.ems.subject.dto;

import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;

// Data Transfer Object, to communicate with frontend.

public record SubjectDto(
		long id,
		String code,
		String name) {
	public SubjectDto(Subject subject) {
		this(subject.getId(), subject.getCode(), subject.getName());
	}
}
