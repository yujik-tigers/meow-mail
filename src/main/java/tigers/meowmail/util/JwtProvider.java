package tigers.meowmail.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.config.JwtProperties;
import tigers.meowmail.exception.InvalidTokenException;

@Component
@RequiredArgsConstructor
public class JwtProvider {

	private final JwtProperties jwtProperties;
	private SecretKey secretKey;

	@PostConstruct
	private void init() {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateVerificationToken(String email) {
		return generateToken(email, jwtProperties.expiration().verification(), TokenType.VERIFICATION);
	}

	public String generateSubscriptionToken(String email) {
		return generateToken(email, jwtProperties.expiration().subscription(), TokenType.SUBSCRIPTION);
	}

	private String generateToken(String email, long expirationMs, TokenType tokenType) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.subject(email)
			.claim("type", tokenType.name())
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey)
			.compact();
	}

	public String getEmailFrom(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();
	}

	public void validateToken(String token, TokenType expectedType) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

			String tokenType = claims.get("type", String.class);
			if (!expectedType.name().equals(tokenType)) {
				throw new InvalidTokenException("Invalid token type");
			}
		} catch (ExpiredJwtException e) {
			throw new InvalidTokenException("Token has expired");
		} catch (JwtException | IllegalArgumentException e) {
			throw new InvalidTokenException("Invalid token");
		}
	}

	public enum TokenType {
		VERIFICATION,
		SUBSCRIPTION
	}

}
