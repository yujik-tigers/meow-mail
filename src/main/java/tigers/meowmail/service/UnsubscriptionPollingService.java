package tigers.meowmail.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.MailPollingProperties;
import tigers.meowmail.util.ImapMailReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnsubscriptionPollingService {

	private static final String LOG_PREFIX = "[UnsubscriptionPolling]";
	private static final String UNSUBSCRIBE_KEYWORD = "구독 해지";

	private final MailPollingProperties mailPollingProperties;
	private final UnsubscriptionService unsubscriptionService;
	private final UnsubscriptionMailService unsubscriptionMailService;

	// @Scheduled(fixedRateString = "${mail.polling.unsubscribe-rate}")
	public void pollUnsubscriptionMails() {
		try (ImapMailReader reader = new ImapMailReader()) {
			reader.connect(mailPollingProperties.imap(), mailPollingProperties.unsubscribe());

			Message[] unreadMessages = reader.fetchUnreadMessages();
			if (unreadMessages.length > 0) {
				log.info("{} Found {} unread message(s).", LOG_PREFIX, unreadMessages.length);
			}

			for (Message message : unreadMessages) {
				try {
					processUnsubscriptionRequest(reader, message);
					reader.markAsRead(message);
				} catch (Exception e) {
					log.error("{} Failed to process message: {}", LOG_PREFIX, reader.getSubjectSafe(message), e);
				}
			}
		} catch (MessagingException e) {
			log.error("{} IMAP connection error", LOG_PREFIX, e);
		}
	}

	private void processUnsubscriptionRequest(ImapMailReader reader, Message message) {
		try {
			String senderEmail = reader.getSenderEmail(message);
			String content = reader.getTextContent(message);

			if (senderEmail == null) {
				log.warn("{} Could not extract sender email (Subject: {})",
					LOG_PREFIX, reader.getSubjectSafe(message));
				return;
			}

			if (!content.contains(UNSUBSCRIBE_KEYWORD)) {
				log.info("{} Keyword not found, sending format guide (Subject: {})",
					LOG_PREFIX, reader.getSubjectSafe(message));
				unsubscriptionMailService.sendFormatGuide(senderEmail);
				return;
			}

			UnsubscriptionResult result = unsubscriptionService.unsubscribe(senderEmail);
			unsubscriptionMailService.sendResult(senderEmail, result);
			log.info("{} Processed unsubscription for: {} (result: {})", LOG_PREFIX, senderEmail, result);
		} catch (MessagingException | IOException e) {
			log.error("{} Error processing message (Subject: {})",
				LOG_PREFIX, reader.getSubjectSafe(message), e);
		}
	}

}
