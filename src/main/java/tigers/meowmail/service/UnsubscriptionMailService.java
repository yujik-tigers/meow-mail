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

@Slf4j
@Service
@RequiredArgsConstructor
public class UnsubscriptionMailService {

	private static final String LOG_PREFIX = "[UnsubscriptionMail]";
	private static final String FORMAT_GUIDE_SUBJECT = "Meow Mail 구독 해지 형식 안내";
	private static final String FORMAT_GUIDE_TEMPLATE = "unsubscription-format-guide";
	private static final String RESULT_SUBJECT = "Meow Mail 구독 해지 결과 안내";
	private static final String RESULT_TEMPLATE = "unsubscription-result";

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	public void sendResult(String email, UnsubscriptionResult result) {
		Context context = new Context();
		context.setVariable("title", result.getTitle());
		context.setVariable("message", result.getMessage());
		context.setVariable("type", result.getType());

		sendMail(email, RESULT_SUBJECT, RESULT_TEMPLATE, context);
	}

	public void sendFormatGuide(String email) {
		sendMail(email, FORMAT_GUIDE_SUBJECT, FORMAT_GUIDE_TEMPLATE, new Context());
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
		}
	}

}
