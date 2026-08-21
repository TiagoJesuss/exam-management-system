package pt.ulisboa.tecnico.rnl.dei.ems.question.domain;


import jakarta.persistence.*;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;
import lombok.Data;

// Fragment (question image) belonging to an exam.
@Data
@Entity
@Table(
		name = "questions",
		uniqueConstraints = @UniqueConstraint(columnNames = { "exam_id", "number" }))
public class Question {

	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "number", nullable = false)
	private int number;

	@Column(name = "max_score", nullable = false)
	private double maxScore;

	@Column(name = "path")
	private String path;

	@ManyToOne
	@JoinColumn(name = "exam_id", nullable = false)
	private Exam exam;

	protected Question() {
	}

	public Question(Exam exam, int number, double maxScore, String path) {
		this.exam = exam;
		this.number = number;
		this.maxScore = maxScore;
		this.path = path;
	}
}
