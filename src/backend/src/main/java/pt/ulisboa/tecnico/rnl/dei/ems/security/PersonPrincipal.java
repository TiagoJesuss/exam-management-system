package pt.ulisboa.tecnico.rnl.dei.ems.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;

public class PersonPrincipal implements UserDetails {

	private final Person person;

	public PersonPrincipal(Person person) {
		this.person = person;
	}

	public Person getPerson() {
		return person;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_" + person.getType().name()));
		for (Permission permission : RolePermissions.forRole(person.getType())) {
			authorities.add(new SimpleGrantedAuthority(permission.name()));
		}
		return authorities;
	}

	@Override
	public String getPassword() {
		return person.getPassword();
	}

	@Override
	public String getUsername() {
		return person.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
