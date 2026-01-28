package tigers.meowmail.service;

import java.io.IOException;
import java.util.Properties;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.MailProperties;
import tigers.meowmail.util.Validator;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailPollingService {

	private static final String IMAP_HOST = "imap.gmail.com";
	private static final String PROTOCOL = "imaps";
	private static final String FOLDER_INBOX = "INBOX";

	private final MailProperties mailProperties;
	private final VerificationService verificationService;

	@Scheduled(fixedRate = 30000)
	public void pollSubscriptionMails() {
		Properties props = new Properties();
		props.put("mail.store.protocol", PROTOCOL);

		try (Store store = Session.getInstance(props).getStore(PROTOCOL)) {
			store.connect(IMAP_HOST, mailProperties.username(), mailProperties.password());

			try (Folder inbox = store.getFolder(FOLDER_INBOX)) {
				inbox.open(Folder.READ_WRITE);

				// Search for unread messages only
				Message[] unreadMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

				if (unreadMessages.length > 0) {
					log.info("[MailPolling] Found {} unread message(s).", unreadMessages.length);
				}

				for (Message message : unreadMessages) {
					processSubscriptionRequest(message);
					message.setFlag(Flags.Flag.SEEN, true); // Mark as read after processing
				}
			}
		} catch (MessagingException e) {
			log.error("[MailPolling] Failed to connect or access mail folder", e);
		}
	}

	private void processSubscriptionRequest(Message message) {
		try {
			String content = getTextFromMessage(message);

			// Expected format: "email, time"
			String[] tokens = content.split(",");
			if (tokens.length >= 2) {
				String targetEmail = tokens[0].trim();
				String requestTime = tokens[1].trim();

				if (!Validator.isValidEmail(targetEmail)) {
					log.warn("[MailPolling] Invalid email format skipped: {} (Subject: {})",
						targetEmail, getSubjectSafe(message));
					return; // Skip this message
				}

				verificationService.sendVerificationMail(targetEmail, requestTime);
				log.info("[MailPolling] Successfully processed request for: {}", targetEmail);
			} else {
				log.warn("[MailPolling] Invalid message format (Subject: {})", getSubjectSafe(message));
			}
		} catch (MessagingException | IOException e) {
			log.error("[MailPolling] Error occurred while processing message (Subject: {})", getSubjectSafe(message), e);
		}
	}

	/**
	 * Extracts plain text content from various Message types (Simple or Multipart).
	 */
	private String getTextFromMessage(Message message) throws MessagingException, IOException {
		if (message.isMimeType("text/plain")) {
			return message.getContent().toString();
		} else if (message.isMimeType("multipart/*")) {
			MimeMultipart mimeMultipart = (MimeMultipart)message.getContent();
			return getTextFromMimeMultipart(mimeMultipart);
		}

		return "";
	}

	private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < mimeMultipart.getCount(); i++) {
			BodyPart bodyPart = mimeMultipart.getBodyPart(i);
			if (bodyPart.isMimeType("text/plain")) {
				result.append(bodyPart.getContent());
			}
		}

		return result.toString();
	}

	private String getSubjectSafe(Message message) {
		try {
			return message.getSubject();
		} catch (MessagingException e) {
			return "Unknown Subject";
		}
	}

}
