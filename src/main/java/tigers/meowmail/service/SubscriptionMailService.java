package tigers.meowmail.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.SubscriptionProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionMailService {

	private static final String LOG_PREFIX = "[SubscriptionMail]";
	private static final String FORMAT_GUIDE_SUBJECT = "Meow Mail 구독 신청 형식 안내";
	private static final String FORMAT_GUIDE_TEMPLATE = "subscription-format-guide";
	private static final String VERIFICATION_SUBJECT = "Meow Mail 구독 이메일 인증";
	private static final String VERIFICATION_TEMPLATE = "subscription-verification";
	private static final String TIME_CHANGED_SUBJECT = "Meow Mail 구독 시간 변경 완료";
	private static final String TIME_CHANGED_TEMPLATE = "subscription-time-changed";

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final SubscriptionProperties subscriptionProperties;

	public void sendVerificationMail(String email, String preferredTime, String rawToken) {
		String verificationUrl = buildVerificationUrl(rawToken);

		Context context = new Context();
		context.setVariable("preferredTime", preferredTime);
		context.setVariable("verificationUrl", verificationUrl);

		sendMail(email, VERIFICATION_SUBJECT, VERIFICATION_TEMPLATE, context);
	}

	public void sendFormatGuide(String email, String errorMessage) {
		Context context = new Context();
		context.setVariable("errorMessage", errorMessage);

		sendMail(email, FORMAT_GUIDE_SUBJECT, FORMAT_GUIDE_TEMPLATE, context);
	}

	public void sendTimeChangedMail(String email, String newTime) {
		Context context = new Context();
		context.setVariable("newTime", newTime);

		sendMail(email, TIME_CHANGED_SUBJECT, TIME_CHANGED_TEMPLATE, context);
	}

	private String buildVerificationUrl(String rawToken) {
		return subscriptionProperties.baseUrl() + "/subscriptions?token=" + rawToken;
	}

	private void sendMail(String email, String subject, String templateName, Context context) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(email);
			helper.setSubject(subject);

			String htmlContent = templateEngine.process(templateName, context);
			helper.setText(htmlContent, true);

			mailSender.send(message);
			log.info("{} Mail sent to: {} (subject: {})", LOG_PREFIX, email, subject);
		} catch (MessagingException e) {
			log.error("{} Failed to send mail to: {} (subject: {})", LOG_PREFIX, email, subject, e);
			throw new RuntimeException("Failed to send mail", e);
		}
	}

}
