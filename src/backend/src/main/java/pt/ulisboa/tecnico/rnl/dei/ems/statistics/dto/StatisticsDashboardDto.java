package pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto;

import java.util.List;

public record StatisticsDashboardDto(
		List<CountByLabelDto> examsByState,
		List<SubjectAverageDto> averagesBySubject,
		GradingProgressDto gradingProgress,
		ReviewVolumeDto reviewVolume) {
}
