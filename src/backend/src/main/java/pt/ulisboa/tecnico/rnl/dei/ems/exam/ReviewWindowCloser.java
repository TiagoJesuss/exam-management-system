package pt.ulisboa.tecnico.rnl.dei.ems.exam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.rnl.dei.ems.exam.service.ExamService;

@Component
public class ReviewWindowCloser {

	@Autowired
	private ExamService examService;

	@Scheduled(fixedDelayString = "${ems.review.close-check-ms:300000}")
	public void closeExpiredWindows() {
		examService.closeExpiredReviewWindows();
	}
}
