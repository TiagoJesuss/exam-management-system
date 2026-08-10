package pt.ulisboa.tecnico.rnl.dei.ems.auth.dto;

public record LoginResponse(
		String token,
		long expiresInMs,
		AuthUserDto user) {
}
