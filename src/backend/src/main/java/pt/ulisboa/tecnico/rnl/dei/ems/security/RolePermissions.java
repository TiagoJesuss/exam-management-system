package pt.ulisboa.tecnico.rnl.dei.ems.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person.PersonType;

/**
 * Single source of truth mapping each role (PersonType) to the permissions it
 * grants. Change the grants here to change what a role can do.
 */
public final class RolePermissions {

	private static final Map<PersonType, Set<Permission>> ROLE_PERMISSIONS = new EnumMap<>(PersonType.class);

	static {
		// Administrators can do everything, including impersonation.
		ROLE_PERMISSIONS.put(PersonType.ADMINISTRATOR, EnumSet.allOf(Permission.class));

		// School staff manage people but cannot delete or impersonate.
		ROLE_PERMISSIONS.put(PersonType.SCHOOL_STAFF, EnumSet.of(
				Permission.PERSON_READ,
				Permission.PERSON_CREATE,
				Permission.PERSON_UPDATE,
				Permission.STATISTICS_READ));

		// Teachers can read people and statistics.
		ROLE_PERMISSIONS.put(PersonType.TEACHER, EnumSet.of(
				Permission.PERSON_READ,
				Permission.STATISTICS_READ));

		// Students can read the people directory.
		ROLE_PERMISSIONS.put(PersonType.STUDENT, EnumSet.of(
				Permission.PERSON_READ));
	}

	private RolePermissions() {
	}

	public static Set<Permission> forRole(PersonType type) {
		return Collections.unmodifiableSet(
				ROLE_PERMISSIONS.getOrDefault(type, EnumSet.noneOf(Permission.class)));
	}
}
