package pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain;

import java.time.Instant;

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
import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;

// Student request to review a specific exam question within the review deadline.
@Data
@Entity
@Table(
		name = "review_requests",
		uniqueConstraints = @UniqueConstraint(columnNames = { "question_id" }))
public class ReviewRequest {

	public enum ReviewRequestStatus {
		PENDING,
		DISTRIBUTED,
		CLOSED
	}

	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "justification", nullable = false, length = 2000)
	private String justification;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ReviewRequestStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@ManyToOne
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	protected ReviewRequest() {
	}

	public ReviewRequest(Question question, String justification) {
		this.question = question;
		this.justification = justification;
		this.status = ReviewRequestStatus.PENDING;
		this.createdAt = Instant.now();
	}
}
