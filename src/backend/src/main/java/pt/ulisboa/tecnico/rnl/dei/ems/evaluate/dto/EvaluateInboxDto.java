package pt.ulisboa.tecnico.rnl.dei.ems.evaluate.dto;

// Blind inbox/detail row: no student identity.
public record EvaluateInboxDto(
		long id,
		String state,
		String type,
		int questionNumber,
		double maxScore,
		String subjectCode,
		Double grade,
		String justification,
		Double previousGrade) {
}
