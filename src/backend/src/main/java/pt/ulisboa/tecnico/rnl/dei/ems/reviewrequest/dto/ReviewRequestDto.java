package pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.dto;

import java.time.Instant;

import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest;

public record ReviewRequestDto(
		long id,
		int questionNumber,
		String justification,
		String status,
		Instant createdAt) {
	public ReviewRequestDto(ReviewRequest request) {
		this(
				request.getId(),
				request.getQuestion().getNumber(),
				request.getJustification(),
				request.getStatus().toString(),
				request.getCreatedAt());
	}
}
