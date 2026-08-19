package pt.ulisboa.tecnico.rnl.dei.ems.school.domain;


import jakarta.persistence.*;

import lombok.Data;

// Domain class representing a school in the system.
// A school has a name and a list of teachers and students.

@Data
@Entity
@Table(name = "schools")
public class School {

	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "region", nullable = false)
	private String region;

	@Column(name = "code", nullable = false, unique = true)
	private String code;


	protected School() {
	}

	public School(String name, String region, String code) {
		this.name = name;
		this.region = region;
		this.code = code;
	}
}
