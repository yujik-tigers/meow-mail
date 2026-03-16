package tigers.meowmail.service;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.entity.Subscription;
import tigers.meowmail.entity.SubscriptionStatus;
import tigers.meowmail.repository.SubscriptionRepository;
import tigers.meowmail.util.JwtProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	// 구글 개인 계정 기준, 하루 전송 가능한 수신자 수: 100

	private static final String SUBJECT_SUBSCRIPTION_VERIFICATION = "[매일묘일] 구독 이메일 인증";
	private static final String SUBJECT_DAILY_CAT = "[매일묘일] 고양이 편지가 도착했어요 🐾";
	private static final String SUBJECT_ADMIN_OTP = "[매일묘일] 관리자 인증 코드";
	private static final String EMAIL_SUBSCRIPTION_VERIFICATION = "email-subscription-verification";
	private static final String EMAIL_DAILY_QUOTE = "email-daily-quote";
	private static final String EMAIL_DAILY_MEME = "email-daily-meme";
	private static final String EMAIL_ADMIN_OTP = "email-admin-otp";

	// 정렬 순서: quotes는 eng → kor → none, memes는 kor → eng → none
	private static final List<String> QUOTE_ORDER = List.of("quotes-eng", "quotes-kor", "quotes-none");
	private static final List<String> MEME_ORDER = List.of("memes-kor", "memes-eng", "memes-none");

	private final TemplateEngine templateEngine;
	private final JavaMailSender mailSender;
	private final ImageService imageService;
	private final SubscriptionRepository subscriptionRepository;
	private final JwtProvider jwtProvider;
	private final AppProperties appProperties;

	public void sendAdminOtpEmail(String adminEmail, String code) {
		Context context = new Context();
		context.setVariable("code", code);
		sendMail(adminEmail, SUBJECT_ADMIN_OTP, EMAIL_ADMIN_OTP, context);
	}

	public void sendVerificationEmail(String email, String token) {
		String verificationUrl = appProperties.baseUrl() + "/api/subscriptions/verify?token=" + token;

		Context context = new Context();
		context.setVariable("verificationUrl", verificationUrl);

		sendMail(email, SUBJECT_SUBSCRIPTION_VERIFICATION, EMAIL_SUBSCRIPTION_VERIFICATION, context);
	}

	// 정해진 시간에 ACTIVE 구독자에게 메일 발송
	@Scheduled(cron = "${scheduled.send-email-cron}", zone = "${app.timezone}")
	public void sendImageEmail() {
		ZoneId zoneId = ZoneId.of(appProperties.timezone());
		ZonedDateTime nowKst = ZonedDateTime.now(zoneId);
		String today = nowKst.toLocalDate().toString();

		List<Path> imagePaths = imageService.findImagePaths(today);
		if (imagePaths.isEmpty()) {
			log.warn("No images found for today ({}). Fetching now.", today);
			imageService.fetchAndSaveImages(today);
			imagePaths = imageService.findImagePaths(today);
		}
		if (imagePaths.isEmpty()) {
			log.warn("No images for today ({}). Skipping email dispatch.", today);
			return;
		}

		List<Subscription> targets = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
		if (targets.isEmpty()) {
			log.info("No active subscribers");
			return;
		}

		boolean isMeme = imagePaths.stream()
			.anyMatch(p -> p.getFileName().toString().contains("-memes-"));

		log.info("Sending {} email to {} subscriber(s)", isMeme ? "MEME" : "QUOTES", targets.size());

		if (isMeme) {
			sendMemeEmails(today, targets, imagePaths);
		} else {
			sendQuoteEmails(today, targets, imagePaths);
		}
	}

	private void sendMemeEmails(String today, List<Subscription> targets, List<Path> imagePaths) {
		// 존재하는 meme 이미지만 수집 (kor 기본, eng/none은 관리자 추가 요청 시)
		Map<String, FileSystemResource> memeImages = collectImages(today, imagePaths, MEME_ORDER);

		if (memeImages.isEmpty()) {
			log.warn("No meme images found for {}. Skipping email dispatch.", today);
			return;
		}

		log.info("Sending meme email with variants: {}", memeImages.keySet());

		for (Subscription subscriber : targets) {
			Context context = buildEmailContext(today, subscriber.getEmail());
			context.setVariable("memeImages", new ArrayList<>(memeImages.keySet()));
			String htmlContent = templateEngine.process(EMAIL_DAILY_MEME, context);
			sendMailWithImages(subscriber.getEmail(), SUBJECT_DAILY_CAT, htmlContent, memeImages, "meme");
		}
	}

	private void sendQuoteEmails(String today, List<Subscription> targets, List<Path> imagePaths) {
		// 존재하는 quote 이미지만 수집 (eng/kor/none 모두 없으면 발송 안함, 1개 이상이면 발송)
		Map<String, FileSystemResource> quoteImages = collectImages(today, imagePaths, QUOTE_ORDER);

		if (quoteImages.isEmpty()) {
			log.warn("No quote images for {}. Skipping email dispatch.", today);
			return;
		}

		log.info("Sending quote email with variants: {}", quoteImages.keySet());

		for (Subscription subscriber : targets) {
			Context context = buildEmailContext(today, subscriber.getEmail());
			context.setVariable("quoteImages", new ArrayList<>(quoteImages.keySet()));
			String htmlContent = templateEngine.process(EMAIL_DAILY_QUOTE, context);
			sendMailWithImages(subscriber.getEmail(), SUBJECT_DAILY_CAT, htmlContent, quoteImages, "quote");
		}
	}

	// 정해진 순서대로 존재하는 이미지 파일만 LinkedHashMap으로 수집
	private Map<String, FileSystemResource> collectImages(String today, List<Path> imagePaths,
		List<String> order) {
		Map<String, FileSystemResource> result = new LinkedHashMap<>();
		for (String key : order) {
			imagePaths.stream()
				.filter(p -> p.getFileName().toString().startsWith(today + "-" + key + "."))
				.findFirst()
				.ifPresent(path -> result.put(key, new FileSystemResource(path)));
		}
		return result;
	}

	private Context buildEmailContext(String today, String email) {
		String token = jwtProvider.generateSubscriptionToken(email);
		Context context = new Context();
		context.setVariable("date", today);
		context.setVariable("unsubscribeUrl", appProperties.baseUrl() + "/unsubscribe?token=" + token);
		return context;
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

	private void sendMailWithImages(String email, String subject, String htmlContent,
		Map<String, FileSystemResource> imageResources, String type) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_RELATED,
				"UTF-8");

			helper.setTo(email);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			for (Map.Entry<String, FileSystemResource> entry : imageResources.entrySet()) {
				helper.addInline(entry.getKey(), entry.getValue(), toMediaType(entry.getValue().getFilename()));
			}

			mailSender.send(message);
			log.info("Daily {} mail sent to: {}", type, email);
		} catch (MessagingException e) {
			log.error("Failed to send daily {} mail to: {}", type, email, e);
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
