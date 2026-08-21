package pt.ulisboa.tecnico.rnl.dei.ems.question;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.rnl.dei.ems.question.dto.QuestionDto;
import pt.ulisboa.tecnico.rnl.dei.ems.question.service.QuestionService;

@RestController
public class QuestionController {
	@Autowired
	private QuestionService questionService;

	@GetMapping("/questions")
	@PreAuthorize("hasAuthority('QUESTION_READ')")
	public List<QuestionDto> getQuestions() {
		return questionService.getQuestions();
	}

	@GetMapping("/questions/{id}")
	@PreAuthorize("hasAuthority('QUESTION_READ')")
	public QuestionDto getQuestion(@PathVariable long id) {
		return questionService.getQuestion(id);
	}
}
