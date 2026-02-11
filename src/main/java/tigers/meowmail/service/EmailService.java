package tigers.meowmail.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private static final String SUBJECT_SUBSCRIPTION_VERIFICATION = "Subscription email verification";
	private static final String EMAIL_SUBSCRIPTION_VERIFICATION = "email-subscription-verification";

	private final TemplateEngine templateEngine;
	private final JavaMailSender mailSender;

	// TODO: Properties Class
	@Value("${app.base-url}")
	private String baseUrl;

	public void sendVerificationEmail(String email, String token) {
		String verificationUrl = baseUrl + "/api/subscriptions/verify?token=" + token;

		Context context = new Context();
		context.setVariable("verificationUrl", verificationUrl);

		sendMail(email, SUBJECT_SUBSCRIPTION_VERIFICATION, EMAIL_SUBSCRIPTION_VERIFICATION, context);
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
			log.info("Mail sent to: {} (subject: {})", email, subject);
		} catch (MessagingException e) {
			log.error("Failed to send mail to: {} (subject: {})", email, subject, e);
			throw new RuntimeException("Failed to send mail", e);
		}
	}

	// TODO: Create Scheduler 1
	// TODO: Create Scheduler 2

}
