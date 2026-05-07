package tigers.meowmail.config;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.properties.TestMailProperties;
import tigers.meowmail.service.EmailService;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${test-mail.recipient:}' != '' || '${test-mail.recipients:}' != ''")
@Slf4j
public class TestMailRunner implements ApplicationRunner {

	private final EmailService emailService;
	private final TestMailProperties testMailProperties;
	private final ConfigurableApplicationContext applicationContext;

	@Override
	public void run(@NonNull ApplicationArguments args) {
		List<String> recipients = resolveRecipients();
		log.warn("Test daily meme mail dispatch started. recipients={}", recipients);

		boolean allSent = true;
		for (String recipient : recipients) {
			boolean sent = emailService.sendDailyMemeEmailTo(recipient);
			if (sent) {
				log.warn("Test daily meme mail dispatch completed. recipient={}", recipient);
			} else {
				log.error("Test daily meme mail dispatch failed. recipient={}", recipient);
				allSent = false;
			}
		}

		if (testMailProperties.exitAfterSend()) {
			int exitCode = allSent ? 0 : 1;
			log.info("Exiting after test mail dispatch. exitCode={}", exitCode);
			if (!allSent) {
				throw new IllegalStateException("Test daily meme mail dispatch failed");
			}
			SpringApplication.exit(applicationContext, () -> exitCode);
		}
	}

	private List<String> resolveRecipients() {
		List<String> recipients = new ArrayList<>();
		if (testMailProperties.recipient() != null && !testMailProperties.recipient().isBlank()) {
			recipients.add(testMailProperties.recipient().trim());
		}
		if (testMailProperties.recipients() != null) {
			testMailProperties.recipients().stream()
				.filter(recipient -> recipient != null && !recipient.isBlank())
				.map(String::trim)
				.forEach(recipients::add);
		}
		return recipients;
	}

}
