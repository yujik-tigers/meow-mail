package tigers.meowmail.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.config.SubscriptionProperties;
import tigers.meowmail.entity.Subscriber;
import tigers.meowmail.entity.SubscriptionToken;
import tigers.meowmail.repository.SubscriptionTokenRepository;
import tigers.meowmail.util.TokenCodec;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionTokenService {

	private final SubscriptionTokenRepository subscriptionTokenRepo;
	private final SubscriptionProperties subscriptionProperties;
	private final Clock clock;

	public String createToken(Subscriber subscriber) {
		String rawToken = TokenCodec.newRawTokenUrlSafe();
		String tokenHash = TokenCodec.sha256Hex(rawToken);

		Instant now = Instant.now(clock);
		Instant expiresAt = now.plus(subscriptionProperties.token().expiryHours(), ChronoUnit.HOURS);

		SubscriptionToken token = subscriptionTokenRepo.findBySubscriber(subscriber)
			.orElseGet(() -> SubscriptionToken.builder().subscriber(subscriber).build());

		token.updateToken(tokenHash, expiresAt);
		subscriptionTokenRepo.save(token);

		return rawToken;
	}

}
