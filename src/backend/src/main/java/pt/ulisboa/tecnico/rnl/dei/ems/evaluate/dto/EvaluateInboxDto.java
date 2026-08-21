package pt.ulisboa.tecnico.rnl.dei.ems.evaluate.dto;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate;

// Blind inbox/detail row: no student identity.
public record EvaluateInboxDto(
		long id,
		String state,
		String type,
		int questionNumber,
		double maxScore,
		String subjectCode,
		Double grade) {
	public EvaluateInboxDto(Evaluate evaluate) {
		this(
				evaluate.getId(),
				evaluate.getState().toString(),
				evaluate.getType().toString(),
				evaluate.getQuestion().getNumber(),
				evaluate.getQuestion().getMaxScore(),
				evaluate.getQuestion().getExam().getSubject().getCode(),
				evaluate.getGrade());
	}
}
