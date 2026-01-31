package tigers.meowmail.service;

import java.io.IOException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.MailPollingProperties;
import tigers.meowmail.util.ImapMailReader;
import tigers.meowmail.util.Validator;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPollingService {

	private static final String LOG_PREFIX = "[SubscriptionPolling]";
	private static final String ERROR_INVALID_FORMAT = "메일 본문 형식이 올바르지 않습니다. '이메일, 시간' 형식으로 작성해 주세요.";
	private static final String ERROR_INVALID_EMAIL = "이메일 주소 형식이 올바르지 않습니다.";
	private static final String ERROR_INVALID_TIME = "시간 형식이 올바르지 않습니다. HH:mm 형식(예: 09:00)으로 작성해 주세요.";

	private final MailPollingProperties mailPollingProperties;
	private final VerificationService verificationService;
	private final SubscriptionMailService subscriptionMailService;

	//   매번 ImapMailReader.connect()를 호출하는 이유
	//
	//   1. IMAP 연결은 시간이 지나면 타임아웃되거나 끊어질 수 있어 매번 새로 연결하여 끊어진 연결을 재사용하는 문제를 방지
	//   2. try-with-resources로 폴링마다 연결을 열고 닫아 자원 누수 방지
	//   3. 연결을 공유하지 않으므로 멀티스레드 환경에서 동기화 문제 없음
	//   4. 연결 실패 시 다음 폴링에서 자동으로 새 연결을 시도
	//   5. 30s ~ 60s 간격이면 연결을 유지하는 것보다 매번 새로 연결하는 게 효율적
	@Scheduled(fixedRateString = "${mail.polling.subscribe-rate}")
	public void pollSubscriptionMails() {
		try (ImapMailReader reader = new ImapMailReader()) {
			reader.connect(mailPollingProperties.imap(), mailPollingProperties.subscribe());

			Message[] unreadMessages = reader.fetchUnreadMessages();
			if (unreadMessages.length > 0) {
				log.info("{} Found {} unread message(s).", LOG_PREFIX, unreadMessages.length);
			}

			for (Message message : unreadMessages) {
				try {
					processSubscriptionRequest(reader, message);
					reader.markAsRead(message);
				} catch (Exception e) {
					log.error("{} Failed to process message: {}", LOG_PREFIX, reader.getSubjectSafe(message), e);
				}
			}
		} catch (MessagingException e) {
			log.error("{} IMAP connection error", LOG_PREFIX, e);
		}
	}

	private void processSubscriptionRequest(ImapMailReader reader, Message message) {
		try {
			String senderEmail = reader.getSenderEmail(message);
			String content = reader.getTextContent(message);

			String[] tokens = content.split("\\s+");
			if (tokens.length < 2) {
				log.warn("{} Invalid message format (Subject: {})", LOG_PREFIX, reader.getSubjectSafe(message));
				sendFormatGuideIfPossible(senderEmail, ERROR_INVALID_FORMAT);
				return;
			}

			String targetEmail = tokens[0].strip();
			String requestTime = tokens[1].strip();

			if (!Validator.isValidEmail(targetEmail)) {
				log.warn("{} Invalid email format: {} (Subject: {})",
					LOG_PREFIX, targetEmail, reader.getSubjectSafe(message));
				sendFormatGuideIfPossible(senderEmail, ERROR_INVALID_EMAIL);
				return;
			}

			if (!Validator.isValidTime(requestTime)) {
				log.warn("{} Invalid time format: {} (Subject: {})",
					LOG_PREFIX, requestTime, reader.getSubjectSafe(message));
				sendFormatGuideIfPossible(senderEmail, ERROR_INVALID_TIME);
				return;
			}

			verificationService.sendVerificationMail(targetEmail, requestTime);
			log.info("{} Successfully processed request for: {}", LOG_PREFIX, targetEmail);
		} catch (MessagingException | IOException e) {
			log.error("{} Error processing message (Subject: {})",
				LOG_PREFIX, reader.getSubjectSafe(message), e);
		}
	}

	private void sendFormatGuideIfPossible(String senderEmail, String errorMessage) {
		if (senderEmail != null) {
			subscriptionMailService.sendFormatGuide(senderEmail, errorMessage);
		}
	}

}
