package pt.ulisboa.tecnico.rnl.dei.ems.statistics.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.domain.Evaluate.EvaluateState;
import pt.ulisboa.tecnico.rnl.dei.ems.evaluate.repository.EvaluateRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.domain.Exam.ExamState;
import pt.ulisboa.tecnico.rnl.dei.ems.exam.repository.ExamRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.DEIException;
import pt.ulisboa.tecnico.rnl.dei.ems.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;
import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.domain.ReviewRequest.ReviewRequestStatus;
import pt.ulisboa.tecnico.rnl.dei.ems.reviewrequest.repository.ReviewRequestRepository;
import pt.ulisboa.tecnico.rnl.dei.ems.security.SecurityUtils;
import pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto.CountByLabelDto;
import pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto.GradingProgressDto;
import pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto.ReviewVolumeDto;
import pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto.StatisticsDashboardDto;
import pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto.SubjectAverageDto;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private EvaluateRepository evaluateRepository;

	@Autowired
	private ReviewRequestRepository reviewRequestRepository;

	public StatisticsDashboardDto getDashboard() {
		Person current = SecurityUtils.currentPerson();
		if (current == null) {
			throw new DEIException(ErrorMessage.NOT_AUTHENTICATED);
		}

		List<Exam> exams = examRepository.findAll().stream()
				.filter(exam -> inScope(current, exam))
				.toList();
		Set<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());

		List<Evaluate> evaluates = evaluateRepository.findAll().stream()
				.filter(e -> examIds.contains(e.getQuestion().getExam().getId()))
				.toList();

		List<ReviewRequest> reviews = reviewRequestRepository.findAll().stream()
				.filter(r -> examIds.contains(r.getQuestion().getExam().getId()))
				.toList();

		return new StatisticsDashboardDto(
				examsByState(exams),
				averagesBySubject(exams),
				gradingProgress(evaluates),
				reviewVolume(reviews));
	}

	private boolean inScope(Person current, Exam exam) {
		if (current.getType() != PersonType.SCHOOL_STAFF) {
			return true;
		}
		return current.getSchool() != null
				&& exam.getStudent().getSchool() != null
				&& current.getSchool().getId().equals(exam.getStudent().getSchool().getId());
	}

	private List<CountByLabelDto> examsByState(List<Exam> exams) {
		Map<ExamState, Long> counts = new EnumMap<>(ExamState.class);
		for (ExamState state : ExamState.values()) {
			counts.put(state, 0L);
		}
		for (Exam exam : exams) {
			counts.merge(exam.getState(), 1L, Long::sum);
		}
		return Arrays.stream(ExamState.values())
				.map(state -> new CountByLabelDto(state.name(), counts.getOrDefault(state, 0L)))
				.toList();
	}

	private List<SubjectAverageDto> averagesBySubject(List<Exam> exams) {
		Map<String, List<Double>> scoresBySubject = exams.stream()
				.filter(e -> e.getFinalScore() != null)
				.collect(Collectors.groupingBy(
						e -> e.getSubject().getCode(),
						Collectors.mapping(Exam::getFinalScore, Collectors.toList())));

		return scoresBySubject.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> {
					List<Double> scores = entry.getValue();
					double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
					return new SubjectAverageDto(entry.getKey(), round2(avg), scores.size());
				})
				.toList();
	}

	private GradingProgressDto gradingProgress(List<Evaluate> evaluates) {
		long total = evaluates.size();
		long graded = evaluates.stream().filter(e -> e.getState() == EvaluateState.GRADED).count();
		double percent = total == 0 ? 0.0 : round2(100.0 * graded / total);
		return new GradingProgressDto(graded, total, percent);
	}

	private ReviewVolumeDto reviewVolume(List<ReviewRequest> reviews) {
		Map<ReviewRequestStatus, Long> counts = new EnumMap<>(ReviewRequestStatus.class);
		for (ReviewRequestStatus status : ReviewRequestStatus.values()) {
			counts.put(status, 0L);
		}
		for (ReviewRequest review : reviews) {
			counts.merge(review.getStatus(), 1L, Long::sum);
		}
		List<CountByLabelDto> byStatus = Arrays.stream(ReviewRequestStatus.values())
				.map(status -> new CountByLabelDto(status.name(), counts.getOrDefault(status, 0L)))
				.toList();
		return new ReviewVolumeDto(reviews.size(), byStatus);
	}

	private static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
