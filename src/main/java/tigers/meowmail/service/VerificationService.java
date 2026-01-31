package tigers.meowmail.service;

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
public class VerificationService {

	private static final String LOG_PREFIX = "[Verification]";

	private final SubscriberRepository subscriberRepo;
	private final SubscriptionTokenService subscriptionTokenService;
	private final SubscriptionMailService subscriptionMailService;
	private final Clock clock;

	public void sendVerificationMail(String email, String preferredTime) {
		Subscriber subscriber = findOrCreateSubscriber(email, preferredTime);

		if (subscriber.getStatus() == SubscriptionStatus.ACTIVE) {
			if (subscriber.getTime().equals(preferredTime)) {
				log.info("{} Already active subscriber with same time, skipping: {}", LOG_PREFIX, email);
				return;
			}
			subscriber.updateTime(preferredTime, Instant.now(clock));
			subscriptionMailService.sendTimeChangedMail(email, preferredTime);
			log.info("{} Updated subscription time for active subscriber: {} -> {}", LOG_PREFIX, email, preferredTime);
			return;
		}

		String rawToken = subscriptionTokenService.createToken(subscriber);
		subscriptionMailService.sendVerificationMail(email, preferredTime, rawToken);
		log.info("{} Verification mail sent to: {}", LOG_PREFIX, email);
	}

	private Subscriber findOrCreateSubscriber(String email, String preferredTime) {
		String normalizedEmail = email.toLowerCase();
		Optional<Subscriber> existingSubscriber = subscriberRepo.findByEmail(normalizedEmail);

		if (existingSubscriber.isPresent()) {
			Subscriber subscriber = existingSubscriber.get();
			if (subscriber.getStatus() != SubscriptionStatus.ACTIVE) {
				subscriber.updateTime(preferredTime, Instant.now(clock));
			}
			return subscriber;
		}

		Instant now = Instant.now(clock);
		Subscriber newSubscriber = Subscriber.builder()
			.email(normalizedEmail)
			.time(preferredTime)
			.status(SubscriptionStatus.PENDING)
			.createdAt(now)
			.updatedAt(now)
			.build();

		return subscriberRepo.save(newSubscriber);
	}

}
