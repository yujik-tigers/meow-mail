package tigers.meowmail.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.entity.SubscriptionToken;
import tigers.meowmail.repository.SubscriptionTokenRepository;
import tigers.meowmail.util.TokenCodec;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionTokenService {

	private static final long TOKEN_EXPIRY_HOURS = 24;

	private final SubscriptionTokenRepository tokenRepository;
	private final Clock clock;

	/**
	 * 새로운 구독 인증 토큰을 생성하고 저장합니다.
	 *
	 * @param subscriberId 구독자 ID
	 * @return raw token (URL에 포함될 원본 토큰)
	 */
	public String createToken(Long subscriberId) {
		String rawToken = TokenCodec.newRawTokenUrlSafe();
		String tokenHash = TokenCodec.sha256Hex(rawToken);

		Instant now = Instant.now(clock);
		Instant expiresAt = now.plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);

		SubscriptionToken token = SubscriptionToken.builder()
			.subscriberId(subscriberId)
			.tokenHashHex(tokenHash)
			.expiresAt(expiresAt)
			.build();

		tokenRepository.save(token);

		return rawToken;
	}

}
