package pt.ulisboa.tecnico.rnl.dei.ems.subject.domain;


import jakarta.persistence.*;

import lombok.Data;

// Domain class representing a subject in the system.
@Data
@Entity
@Table(name = "subjects")
public class Subject {

	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "code", nullable = false, unique = true)
	private String code;

	protected Subject() {
	}

	public Subject(String name, String code) {
		this.name = name;
		this.code = code;
	}
}
