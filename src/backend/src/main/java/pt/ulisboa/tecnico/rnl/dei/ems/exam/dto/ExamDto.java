package pt.ulisboa.tecnico.rnl.dei.ems.exam.dto;

import java.time.Instant;

import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;

// Data Transfer Object, to communicate with frontend.
public record ExamDto(
		long id,
		String state,
		String path,
		long studentId,
		String studentName,
		String schoolCode,
		String subjectCode,
		Double finalScore,
		Instant reviewDeadline,
		boolean consultationRequested) {
	public ExamDto(Exam exam) {
		this(
				exam.getId(),
				exam.getState().toString(),
				exam.getPath(),
				exam.getStudent().getId(),
				exam.getStudent().getName(),
				exam.getStudent().getSchool() != null ? exam.getStudent().getSchool().getCode() : null,
				exam.getSubject().getCode(),
				exam.getFinalScore(),
				exam.getReviewDeadline(),
				exam.isConsultationRequested());
	}
}
