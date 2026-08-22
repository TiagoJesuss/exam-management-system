package pt.ulisboa.tecnico.rnl.dei.ems.exam.dto;

import java.time.Instant;
import java.util.List;

public record ExamConsultationDto(
		long examId,
		String state,
		String subjectCode,
		Double finalScore,
		Instant reviewDeadline,
		boolean consultationRequested,
		List<FragmentGradeDto> fragments) {
}
