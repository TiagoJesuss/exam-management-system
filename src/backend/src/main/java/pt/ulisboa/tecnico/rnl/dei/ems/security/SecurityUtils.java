package pt.ulisboa.tecnico.rnl.dei.ems.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static PersonPrincipal currentPrincipal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof PersonPrincipal principal) {
			return principal;
		}
		return null;
	}

	public static Person currentPerson() {
		PersonPrincipal principal = currentPrincipal();
		return principal == null ? null : principal.getPerson();
	}
}
