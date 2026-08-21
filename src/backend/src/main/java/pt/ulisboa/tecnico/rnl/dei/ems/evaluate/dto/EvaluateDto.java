package pt.ulisboa.tecnico.rnl.dei.ems.evaluate.dto;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate;

public record EvaluateDto(
		long id,
		Double grade,
		String state,
		String type,
		long professorId,
		long questionId) {
	public EvaluateDto(Evaluate evaluate) {
		this(
				evaluate.getId(),
				evaluate.getGrade(),
				evaluate.getState().toString(),
				evaluate.getType().toString(),
				evaluate.getProfessor().getId(),
				evaluate.getQuestion().getId());
	}
}
