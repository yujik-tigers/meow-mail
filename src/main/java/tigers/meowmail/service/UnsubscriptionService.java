package tigers.meowmail.service;

import static tigers.meowmail.service.UnsubscriptionResult.ALREADY_INACTIVE;
import static tigers.meowmail.service.UnsubscriptionResult.NOT_FOUND;
import static tigers.meowmail.service.UnsubscriptionResult.SUCCESS;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.entity.Subscriber;
import tigers.meowmail.entity.SubscriptionStatus;
import tigers.meowmail.repository.SubscriberRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UnsubscriptionService {

	private static final String LOG_PREFIX = "[Unsubscribe]";

	private final SubscriberRepository subscriberRepo;
	private final Clock clock;

	public UnsubscriptionResult unsubscribe(String email) {
		String normalizedEmail = email.toLowerCase();
		Optional<Subscriber> subscriberOpt = subscriberRepo.findByEmail(normalizedEmail);

		if (subscriberOpt.isEmpty()) {
			log.warn("{} Subscriber not found: {}", LOG_PREFIX, normalizedEmail);
			return NOT_FOUND;
		}

		Subscriber subscriber = subscriberOpt.get();
		if (subscriber.getStatus() == SubscriptionStatus.INACTIVE) {
			log.info("{} Already inactive: {}", LOG_PREFIX, normalizedEmail);
			return ALREADY_INACTIVE;
		}

		subscriber.markInactive(Instant.now(clock));
		log.info("{} Unsubscribed: {}", LOG_PREFIX, normalizedEmail);
		return SUCCESS;
	}

}
