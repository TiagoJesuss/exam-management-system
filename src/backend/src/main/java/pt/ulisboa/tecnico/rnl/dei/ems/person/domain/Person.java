package pt.ulisboa.tecnico.rnl.dei.ems.person.domain;


import jakarta.persistence.*;
import pt.ulisboa.tecnico.rnl.dei.ems.school.domain.School;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;
import lombok.Data;
import lombok.ToString;

// Domain class representing a person in the system.
// A person is also a login account: it has an email + password, and its
// PersonType is the security role that determines what it is allowed to do.
@Data
@Entity
@Table(name = "people")
public class Person {

	public enum PersonType {
		ADMINISTRATOR,
		SCHOOL_STAFF,
		TEACHER,
		STUDENT
	}

	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	// Login identifier, unique across all accounts.
	@Column(name = "email", nullable = false, unique = true)
	private String email;

	// BCrypt hash of the password; excluded from toString so it is never logged.
	@ToString.Exclude
	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
    private PersonType type;

	@ManyToOne
	@JoinColumn(name = "school_id")
	private School school;

	@ManyToOne
	@JoinColumn(name = "subject_id")
	private Subject subject;

	protected Person() {
	}

	public Person(String name, String email, String password, PersonType type, School school) {
		this(name, email, password, type, school, null);

	}

	public Person(String name, String email, String password, PersonType type, School school, Subject subject) {
		this(name, email, password, type);
		this.school = school;
		this.subject = subject;
	}

	public Person(String name, String email, String password, PersonType type) {
		this.name = name;
		this.email = email;
		this.password = password;
		this.type = type;
	}
}
