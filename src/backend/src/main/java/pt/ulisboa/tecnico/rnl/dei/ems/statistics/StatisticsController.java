package pt.ulisboa.tecnico.rnl.dei.ems.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto.StatisticsDashboardDto;
import pt.ulisboa.tecnico.rnl.dei.ems.statistics.service.StatisticsService;

@RestController
public class StatisticsController {

	@Autowired
	private StatisticsService statisticsService;

	@GetMapping("/statistics")
	@PreAuthorize("hasAuthority('STATISTICS_READ')")
	public StatisticsDashboardDto getDashboard() {
		return statisticsService.getDashboard();
	}
}
