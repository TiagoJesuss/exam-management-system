package pt.ulisboa.tecnico.rnl.dei.ems.school.dto;

import pt.ulisboa.tecnico.rnl.dei.ems.school.domain.School;

// Data Transfer Object, to communicate with frontend.


public record SchoolDto(
		long id,
		String name,
		String region,
		String code) {
	public SchoolDto(School school) {
		this(school.getId(), school.getName(), school.getRegion(), school.getCode());
	}
}
