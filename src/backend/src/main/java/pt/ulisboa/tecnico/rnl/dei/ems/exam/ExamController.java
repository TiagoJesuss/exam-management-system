package pt.ulisboa.tecnico.rnl.dei.ems.exam;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.CloseReviewWindowsResultDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.DistributionResultDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.ExamConsultationDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.dto.ExamDto;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.service.ExamService;

@RestController
public class ExamController {
	@Autowired
	private ExamService examService;

	@GetMapping("/exams")
	@PreAuthorize("hasAuthority('EXAM_READ')")
	public List<ExamDto> getExams() {
		return examService.getExams();
	}

	@GetMapping("/exams/mine")
	@PreAuthorize("hasAuthority('EXAM_CONSULT')")
	public List<ExamDto> getMyExams() {
		return examService.getMyConsultableExams();
	}

	@GetMapping("/exams/{id}")
	@PreAuthorize("hasAuthority('EXAM_READ')")
	public ExamDto getExam(@PathVariable long id) {
		return examService.getExam(id);
	}

	@GetMapping("/exams/{id}/consultation")
	@PreAuthorize("hasAuthority('EXAM_CONSULT')")
	public ExamConsultationDto getConsultation(@PathVariable long id) {
		return examService.getConsultation(id);
	}

	@GetMapping("/exams/{id}/file")
	@PreAuthorize("hasAnyAuthority('EXAM_READ', 'EXAM_CONSULT')")
	public ResponseEntity<Resource> getExamFile(@PathVariable long id) {
		Path path = examService.getExamFilePath(id);
		Resource resource = new FileSystemResource(path);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"exam-" + id + ".pdf\"")
				.body(resource);
	}

	@PostMapping("/exams")
	@PreAuthorize("hasAuthority('EXAM_CREATE')")
	public ExamDto uploadExam(
			@RequestParam("file") MultipartFile file,
			@RequestParam("studentId") long studentId,
			@RequestParam("subjectCode") String subjectCode) {
		return examService.uploadExam(file, studentId, subjectCode);
	}

	@PostMapping("/exams/lock")
	@PreAuthorize("hasAuthority('EXAM_LOCK')")
	public List<ExamDto> lockSegmentedExams() {
		return examService.lockSegmentedExams();
	}

	@PostMapping("/exams/distribute")
	@PreAuthorize("hasAuthority('EXAM_DISTRIBUTE')")
	public DistributionResultDto distributeLockedExams() {
		return examService.distributeLockedExams();
	}

	@PostMapping("/exams/close-review-windows")
	@PreAuthorize("hasAuthority('EXAM_LOCK')")
	public CloseReviewWindowsResultDto closeExpiredReviewWindows() {
		return examService.forceCloseReviewWindows();
	}

	@PostMapping("/exams/publish-grades")
	@PreAuthorize("hasAuthority('EXAM_RELEASE')")
	public List<ExamDto> publishGrades(
			@RequestParam(value = "schoolCode", required = false) String schoolCode,
			@RequestParam("subjectCode") String subjectCode) {
		return examService.publishGrades(schoolCode, subjectCode);
	}

	@PostMapping("/exams/release")
	@PreAuthorize("hasAuthority('EXAM_RELEASE')")
	public List<ExamDto> releaseExams(
			@RequestParam("schoolCode") String schoolCode,
			@RequestParam("subjectCode") String subjectCode) {
		return examService.releaseExams(schoolCode, subjectCode);
	}

	@PostMapping("/exams/{id}/release")
	@PreAuthorize("hasAuthority('EXAM_RELEASE')")
	public ExamDto releaseExam(@PathVariable long id) {
		return examService.releaseExam(id);
	}

	@PostMapping("/exams/{id}/request-consultation")
	@PreAuthorize("hasAuthority('EXAM_CONSULT')")
	public ExamDto requestConsultation(@PathVariable long id) {
		return examService.requestConsultation(id);
	}

	@PostMapping("/exams/{id}/segment")
	@PreAuthorize("hasAuthority('QUESTION_CREATE')")
	public ExamDto segmentExam(
			@PathVariable long id,
			@RequestParam("files") MultipartFile[] files,
			@RequestParam("maxScores") Double[] maxScores) {
		return examService.segmentExam(id, files, maxScores);
	}
}
