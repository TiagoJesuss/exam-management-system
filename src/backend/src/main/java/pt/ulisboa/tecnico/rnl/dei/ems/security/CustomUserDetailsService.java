package pt.ulisboa.tecnico.rnl.dei.ems.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.rnl.dei.ems.person.repository.PersonRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final PersonRepository personRepository;

	public CustomUserDetailsService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return personRepository.findByEmail(email)
				.map(PersonPrincipal::new)
				.orElseThrow(() -> new UsernameNotFoundException("No account for email: " + email));
	}
}
