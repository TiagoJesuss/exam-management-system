package pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.dto.CreateReviewRequestDto;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.dto.ReviewRequestDto;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.service.ReviewRequestService;

@RestController
public class ReviewRequestController {

	@Autowired
	private ReviewRequestService reviewRequestService;

	@GetMapping("/exams/{id}/review-requests")
	@PreAuthorize("hasAuthority('EXAM_CONSULT')")
	public List<ReviewRequestDto> listForExam(@PathVariable long id) {
		return reviewRequestService.listForExam(id);
	}

	@PostMapping("/exams/{id}/review-requests")
	@PreAuthorize("hasAuthority('EXAM_CONSULT')")
	public ReviewRequestDto create(
			@PathVariable long id,
			@RequestBody CreateReviewRequestDto request) {
		return reviewRequestService.create(id, request);
	}
}
