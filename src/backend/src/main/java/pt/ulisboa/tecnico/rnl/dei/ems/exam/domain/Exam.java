package pt.ulisboa.tecnico.rnl.dei.ems.exam.domain;


import jakarta.persistence.*;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.subject.domain.Subject;
import lombok.Data;

// Domain class representing an exam PDF uploaded for a student and subject.
@Data
@Entity
@Table(name = "exams")
public class Exam {

	public enum ExamState {
		UPLOADED,
		SEGMENTED,
		LOCKED,
		IN_GRADING,
		GRADED
	}

	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "state", nullable = false)
	@Enumerated(EnumType.STRING)
	private ExamState state;

	@Column(name = "path")
	private String path;

	@Column(name = "final_score")
	private Double finalScore;

	@ManyToOne
	@JoinColumn(name = "student_id", nullable = false)
	private Person student;

	@ManyToOne
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	protected Exam() {
	}

	public Exam(Person student, Subject subject, String path) {
		this.student = student;
		this.subject = subject;
		this.path = path;
		this.state = ExamState.UPLOADED;
	}
}
