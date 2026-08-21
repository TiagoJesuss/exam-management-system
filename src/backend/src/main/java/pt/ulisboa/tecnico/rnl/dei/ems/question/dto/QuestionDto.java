package pt.ulisboa.tecnico.rnl.dei.ems.question.dto;

import pt.ulisboa.tecnico.rnl.dei.ems.question.domain.Question;

// Data Transfer Object, to communicate with frontend.
public record QuestionDto(
		long id,
		int number,
		double maxScore,
		String path,
		long examId) {
	public QuestionDto(Question question) {
		this(
				question.getId(),
				question.getNumber(),
				question.getMaxScore(),
				question.getPath(),
				question.getExam().getId());
	}
}
