package pt.ulisboa.tecnico.rnl.dei.ems.statistics.dto;

import java.util.List;

public record ReviewVolumeDto(long total, List<CountByLabelDto> byStatus) {
}
