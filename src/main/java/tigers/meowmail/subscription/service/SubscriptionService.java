package tigers.meowmail.subscription.service;

import static tigers.meowmail.subscription.service.SubscriptionConfirmResult.ALREADY_ACTIVE;
import static tigers.meowmail.subscription.service.SubscriptionConfirmResult.CONFIRMED;
import static tigers.meowmail.subscription.service.SubscriptionConfirmResult.SUBSCRIBER_INACTIVE;
import static tigers.meowmail.subscription.service.SubscriptionConfirmResult.SUBSCRIBER_NOT_FOUND;
import static tigers.meowmail.subscription.service.SubscriptionConfirmResult.TOKEN_EXPIRED;
import static tigers.meowmail.subscription.service.SubscriptionConfirmResult.TOKEN_INVALID;
import static tigers.meowmail.subscription.service.SubscriptionConfirmResult.TOKEN_USED;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.subscription.entity.Subscriber;
import tigers.meowmail.subscription.entity.SubscriptionStatus;
import tigers.meowmail.subscription.entity.SubscriptionToken;
import tigers.meowmail.subscription.repository.SubscriberRepository;
import tigers.meowmail.subscription.repository.SubscriptionTokenRepository;
import tigers.meowmail.util.TokenCodec;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionService {

	private final SubscriptionTokenRepository subscriptionTokenRepo;
	private final SubscriberRepository subscriberRepo;
	private final Clock clock;

	public SubscriptionConfirmResult confirm(String rawToken) {
		String tokenHashHex = TokenCodec.sha256Hex(rawToken);

		Optional<SubscriptionToken> tokenOpt = subscriptionTokenRepo.findByTokenHashHex(tokenHashHex);
		if (tokenOpt.isEmpty())
			return TOKEN_INVALID;

		SubscriptionToken token = tokenOpt.get();

		Instant now = Instant.now(clock);
		if (token.isUsed())
			return TOKEN_USED;
		if (token.isExpired(now))
			return TOKEN_EXPIRED;

		Optional<Subscriber> subscriberOpt = subscriberRepo.findById(token.getSubscriberId());
		if (subscriberOpt.isEmpty())
			return SUBSCRIBER_NOT_FOUND;

		Subscriber subscriber = subscriberOpt.get();

		if (subscriber.getStatus() == SubscriptionStatus.INACTIVE)
			return SUBSCRIBER_INACTIVE;
		if (subscriber.getStatus() == SubscriptionStatus.ACTIVE) {
			token.markUsed(Instant.now(clock));
			return ALREADY_ACTIVE;
		}

		subscriber.markActive(Instant.now(clock));
		token.markUsed(Instant.now(clock));
		return CONFIRMED;
	}

}
