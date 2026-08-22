package pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;

// Assignment of a question fragment to a professor for grading.
@Data
@Entity
@Table(
		name = "evaluates",
		uniqueConstraints = @UniqueConstraint(columnNames = { "question_id", "type" }))
public class Evaluate {

	public enum EvaluateState {
		PENDING,
		IN_PROGRESS,
		GRADED
	}

	public enum EvaluateType {
		INITIAL,
		REVIEW
	}

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	@JoinColumn(name = "professor_id", nullable = false)
	private Person professor;

	@ManyToOne
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(name = "grade")
	private Double grade;

	@Column(name = "state", nullable = false)
	@Enumerated(EnumType.STRING)
	private EvaluateState state;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private EvaluateType type;

	protected Evaluate() {
	}

	public Evaluate(Person professor, Question question, EvaluateType type) {
		this.professor = professor;
		this.question = question;
		this.type = type;
		this.state = EvaluateState.PENDING;
	}
}
