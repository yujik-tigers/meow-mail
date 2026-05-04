package tigers.meowmail.service;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private static final String SUBJECT_DAILY_CAT = "[매일묘일] 오늘의 Cat-phrase 🐾";
	private static final String EMAIL_SUBSCRIPTION_VERIFICATION = "email-subscription-verification";
	private static final String EMAIL_DAILY_MEME = "email-daily-meme";
	private static final String DAILY_MEME_ASSET_CID = "dailyMemeAsset";
	private static final int DAILY_MAIL_BATCH_SIZE = 10;
	private static final long DAILY_MAIL_BATCH_INTERVAL_MILLIS = 30_000L;

	private final TemplateEngine templateEngine;
	private final JavaMailSender mailSender;
	private final ContentService contentService;
	private final SubscriptionRepository subscriptionRepository;
	private final JwtProvider jwtProvider;
	private final AppProperties appProperties;

	public void sendVerificationEmail(String email, String token) {
		String verificationUrl = appProperties.baseUrl() + "/api/subscriptions/verify?token=" + token;

		Context context = new Context();
		context.setVariable("verificationUrl", verificationUrl);

		sendMail(email, SUBJECT_SUBSCRIPTION_VERIFICATION, EMAIL_SUBSCRIPTION_VERIFICATION, context);
	}

	// 정해진 시간에 ACTIVE 구독자에게 메일 발송
	@Scheduled(cron = "${schedule.send-email-cron:0 0 8 * * *}", zone = "${app.timezone}")
	public void sendDailyMemeEmail() {
		ZoneId zoneId = ZoneId.of(appProperties.timezone());
		ZonedDateTime nowKst = ZonedDateTime.now(zoneId);
		String today = nowKst.toLocalDate().toString();

		Optional<Path> assetPath = contentService.findDailyMemeAssetPath(today);
		Optional<DailyMemeContent> content = contentService.findDailyMemeContent(today);
		if (assetPath.isEmpty() || content.isEmpty()) {
			log.warn("No complete daily meme content found for today ({}). Fetching now.", today);
			contentService.fetchAndSaveDailyMemeContent(today);
			assetPath = contentService.findDailyMemeAssetPath(today);
			content = contentService.findDailyMemeContent(today);
		}
		if (assetPath.isEmpty() || content.isEmpty()) {
			log.warn("No complete daily meme content for today ({}). Skipping email dispatch.", today);
			return;
		}

		try {
			List<Subscription> targets = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
			if (targets.isEmpty()) {
				log.info("No active subscribers for today ({}). Skipping email dispatch.", today);
				return;
			}

			sendMemeEmails(today, targets, assetPath.get(), content.get());
		} finally {
			contentService.deleteDailyMemeContent(today);
		}
	}

	private void sendMemeEmails(String today, List<Subscription> targets, Path assetPath, DailyMemeContent content) {
		FileSystemResource memeAsset = new FileSystemResource(assetPath);
		log.info("Sending daily meme email with asset: {}, total targets={}", assetPath.getFileName(), targets.size());

		int successCount = 0;
		int failureCount = 0;
		try (ExecutorService executor = Executors.newFixedThreadPool(DAILY_MAIL_BATCH_SIZE)) {
			for (int start = 0; start < targets.size(); start += DAILY_MAIL_BATCH_SIZE) {
				int end = Math.min(start + DAILY_MAIL_BATCH_SIZE, targets.size());
				List<Subscription> batch = targets.subList(start, end);
				log.info("Sending daily meme mail batch: {}-{} of {}", start + 1, end, targets.size());

				List<CompletableFuture<Boolean>> futures = batch.stream()
					.map(subscriber -> CompletableFuture.supplyAsync(
						() -> sendMemeEmail(today, subscriber, memeAsset, content), executor))
					.toList();

				for (CompletableFuture<Boolean> future : futures) {
					if (future.join()) {
						successCount++;
					} else {
						failureCount++;
					}
				}

				if (end < targets.size() && !sleepBeforeNextBatch()) {
					failureCount += targets.size() - end;
					log.warn("Daily meme dispatch interrupted after {} of {} target(s)", end, targets.size());
					break;
				}
			}
		}
		log.info("Daily meme dispatch completed: total={}, success={}, failure={}", targets.size(), successCount, failureCount);
	}

	private boolean sendMemeEmail(String today, Subscription subscriber, FileSystemResource memeAsset, DailyMemeContent content) {
		try {
			Context context = buildEmailContext(today, subscriber.getEmail());
			context.setVariable("memeAssetCid", DAILY_MEME_ASSET_CID);
			context.setVariable("memeText", content.memeText());
			context.setVariable("expressions", content.expressions());
			context.setVariable("translation", content.translation());
			context.setVariable("background", content.background());
			context.setVariable("author", content.author());
			context.setVariable("source", content.source());
			String htmlContent = templateEngine.process(EMAIL_DAILY_MEME, context);
			return sendMailWithInlineResources(subscriber.getEmail(), SUBJECT_DAILY_CAT, htmlContent,
				Map.of(DAILY_MEME_ASSET_CID, memeAsset), "meme");
		} catch (RuntimeException e) {
			log.error("Failed to prepare daily meme mail to: {}", subscriber.getEmail(), e);
			return false;
		}
	}

	private boolean sleepBeforeNextBatch() {
		try {
			Thread.sleep(DAILY_MAIL_BATCH_INTERVAL_MILLIS);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
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

	private boolean sendMailWithInlineResources(String email, String subject, String htmlContent,
		Map<String, FileSystemResource> inlineResources, String type) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_RELATED,
				"UTF-8");

			helper.setTo(email);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			for (Map.Entry<String, FileSystemResource> entry : inlineResources.entrySet()) {
				helper.addInline(entry.getKey(), entry.getValue(), toMediaType(entry.getValue().getFilename()));
			}

			mailSender.send(message);
			log.info("Daily {} mail sent to: {}", type, email);
			return true;
		} catch (MessagingException e) {
			log.error("Failed to send daily {} mail to: {}", type, email, e);
			return false;
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
