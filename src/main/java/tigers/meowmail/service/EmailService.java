package tigers.meowmail.service;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.entity.Subscription;
import tigers.meowmail.entity.SubscriptionStatus;
import tigers.meowmail.repository.SubscriptionRepository;
import tigers.meowmail.util.JwtProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final String SUBJECT_SUBSCRIPTION_VERIFICATION = "Subscription email verification";
	private static final String SUBJECT_DAILY_CAT = "오늘의 고양이 사진이 도착했어요 🐾";
	private static final String EMAIL_SUBSCRIPTION_VERIFICATION = "email-subscription-verification";
	private static final String EMAIL_DAILY_CAT = "email-daily-cat";
	private static final String IMAGE_CONTENT_ID = "catImage";

	private final TemplateEngine templateEngine;
	private final JavaMailSender mailSender;
	private final ImageService imageService;
	private final SubscriptionRepository subscriptionRepository;
	private final JwtProvider jwtProvider;

	// TODO: Properties Class
	@Value("${app.base-url}")
	private String baseUrl;

	public void sendVerificationEmail(String email, String token) {
		String verificationUrl = baseUrl + "/api/subscriptions/verify?token=" + token;

		Context context = new Context();
		context.setVariable("verificationUrl", verificationUrl);

		sendMail(email, SUBJECT_SUBSCRIPTION_VERIFICATION, EMAIL_SUBSCRIPTION_VERIFICATION, context);
	}

	// 매 시 00분, 30분마다 해당 시각을 구독 시간으로 설정한 ACTIVE 구독자에게 발송
	@Scheduled(cron = "0 0,30 * * * *", zone = "Asia/Seoul")
	public void sendImageEmail() {
		ZonedDateTime nowKst = ZonedDateTime.now(KST);
		// String currentTime = nowKst.format(TIME_FORMATTER); // "HH:mm"
		String currentTime = "16:30"; // "HH:mm"
		String today = nowKst.toLocalDate().toString();  // "YYYY-MM-DD"

		Path imagePath = imageService.findImagePath(today).orElse(null);
		if (imagePath == null) {
			log.warn("No image found for today ({}). Skipping email dispatch.", today);
			return;
		}

		List<Subscription> targets = subscriptionRepository.findByStatusAndTime(SubscriptionStatus.ACTIVE, currentTime);
		if (targets.isEmpty()) {
			log.debug("No active subscribers for time {}", currentTime);
			return;
		}

		log.info("Sending image email to {} subscriber(s) at {}", targets.size(), currentTime);

		FileSystemResource imageResource = new FileSystemResource(imagePath);

		for (Subscription subscriber : targets) {
			String token = jwtProvider.generateSubscriptionToken(subscriber.getEmail());

			Context context = new Context();
			context.setVariable("date", today);
			context.setVariable("resubscribeUrl", baseUrl + "/resubscribe?token=" + token);
			context.setVariable("unsubscribeUrl", baseUrl + "/unsubscribe?token=" + token);

			String htmlContent = templateEngine.process(EMAIL_DAILY_CAT, context);
			sendMailWithInlineImage(subscriber.getEmail(), SUBJECT_DAILY_CAT, htmlContent, imageResource);
		}
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

	private void sendMailWithInlineImage(String email, String subject, String htmlContent, FileSystemResource imageResource) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_RELATED, "UTF-8");

			helper.setTo(email);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			helper.addInline(IMAGE_CONTENT_ID, imageResource, toMediaType(imageResource.getFilename()));

			mailSender.send(message);
			log.info("Daily cat image mail sent to: {}", email);
		} catch (MessagingException e) {
			log.error("Failed to send daily cat image mail to: {}", email, e);
		}
	}

	private static String toMediaType(String filename) {
		if (filename == null)
			return "image/png";
		String lower = filename.toLowerCase();
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
			return "image/jpeg";
		if (lower.endsWith(".gif"))
			return "image/gif";
		if (lower.endsWith(".webp"))
			return "image/webp";
		return "image/png";
	}

}
