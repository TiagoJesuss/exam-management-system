package pt.ulisboa.tecnico.rnl.dei.ems.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import pt.ulisboa.tecnico.rnl.dei.ems.person.domain.Person;

@Service
public class JwtService {

	public static final String CLAIM_UID = "uid";
	public static final String CLAIM_NAME = "name";
	public static final String CLAIM_ROLE = "role";
	public static final String CLAIM_IMPERSONATING = "impersonating";
	public static final String CLAIM_IMPERSONATOR_EMAIL = "impersonatorEmail";
	public static final String CLAIM_IMPERSONATOR_NAME = "impersonatorName";

	@Value("${security.jwt.secret-key}")
	private String secretKey;

	@Value("${security.jwt.expiration-ms}")
	private long jwtExpirationMs;

	public long getExpirationMs() {
		return jwtExpirationMs;
	}

	public String generateToken(Person person) {
		return generateToken(person, new HashMap<>());
	}

	public String generateToken(Person person, Map<String, Object> extraClaims) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtExpirationMs);

		return Jwts.builder()
				.claims(extraClaims)
				.subject(person.getEmail())
				.claim(CLAIM_UID, person.getId())
				.claim(CLAIM_NAME, person.getName())
				.claim(CLAIM_ROLE, person.getType().name())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(getSignInKey())
				.compact();
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> resolver) {
		return resolver.apply(extractAllClaims(token));
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSignInKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		try {
			final String username = extractUsername(token);
			return username.equals(userDetails.getUsername()) && !isExpired(token);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isExpired(String token) {
		return extractClaim(token, Claims::getExpiration).before(new Date());
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
