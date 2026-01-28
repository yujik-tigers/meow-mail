package tigers.meowmail.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.AppProperties;
import tigers.meowmail.entity.Subscriber;
import tigers.meowmail.entity.SubscriptionStatus;
import tigers.meowmail.repository.SubscriberRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VerificationService {

	private static final String MAIL_SUBJECT = "Meow Mail 구독 인증";
	private static final String TEMPLATE_NAME = "subscription-verification";

	private final SubscriberRepository subscriberRepository;
	private final SubscriptionTokenService tokenService;
	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final AppProperties appProperties;
	private final Clock clock;

	public void sendVerificationMail(String email, String preferredTime) {
		Subscriber subscriber = findOrCreateSubscriber(email, preferredTime);

		if (subscriber.getStatus() == SubscriptionStatus.ACTIVE) {
			log.info("[Verification] Already active subscriber, skipping: {}", email);
			return;
		}

		String rawToken = tokenService.createToken(subscriber.getId());
		String verificationUrl = buildVerificationUrl(rawToken);

		sendHtmlMail(email, preferredTime, verificationUrl);
		log.info("[Verification] Verification mail sent to: {}", email);
	}

	private Subscriber findOrCreateSubscriber(String email, String preferredTime) {
		String normalizedEmail = email.toLowerCase();
		Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmail(normalizedEmail);

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

		return subscriberRepository.save(newSubscriber);
	}

	private String buildVerificationUrl(String rawToken) {
		return appProperties.baseUrl() + "/subscriptions?token=" + rawToken;
	}

	private void sendHtmlMail(String toEmail, String preferredTime, String verificationUrl) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(toEmail);
			helper.setSubject(MAIL_SUBJECT);

			String htmlContent = buildHtmlContent(preferredTime, verificationUrl);
			helper.setText(htmlContent, true);

			mailSender.send(message);
		} catch (MessagingException e) {
			log.error("[Verification] Failed to send verification mail to: {}", toEmail, e);
			throw new RuntimeException("Failed to send verification mail", e);
		}
	}

	private String buildHtmlContent(String preferredTime, String verificationUrl) {
		Context context = new Context();
		context.setVariable("preferredTime", preferredTime);
		context.setVariable("verificationUrl", verificationUrl);

		return templateEngine.process(TEMPLATE_NAME, context);
	}

}
