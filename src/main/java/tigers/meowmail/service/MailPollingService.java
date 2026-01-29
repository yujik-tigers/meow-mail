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
		props.put("mail.imaps.timeout", "10000");
		props.put("mail.imaps.connectiontimeout", "10000");

		Store store = null;
		Folder inbox = null;

		try {
			store = Session.getInstance(props).getStore(PROTOCOL);
			store.connect(IMAP_HOST, mailProperties.username(), mailProperties.password());

			inbox = store.getFolder(FOLDER_INBOX);
			inbox.open(Folder.READ_WRITE);

			Message[] unreadMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

			if (unreadMessages.length > 0) {
				log.info("[MailPolling] Found {} unread message(s).", unreadMessages.length);
			}

			for (Message message : unreadMessages) {
				try {
					processSubscriptionRequest(message);
					message.setFlag(Flags.Flag.SEEN, true); // 성공 시에만 읽음 표시
				} catch (Exception e) {
					log.error("[MailPolling] Failed to process message: {}", getSubjectSafe(message), e);
					// 실패 시 다음 폴링에서 재시도하도록 SEEN 플래그를 건드리지 않음
				}
			}
		} catch (MessagingException e) {
			log.error("[MailPolling] IMAP connection error", e);
		} finally {
			// 자원 해제 순서 준수
			try {
				if (inbox != null && inbox.isOpen())
					inbox.close(false);
			} catch (Exception e) { /* ignore */ }
			try {
				if (store != null)
					store.close();
			} catch (Exception e) { /* ignore */ }
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

	private String getTextFromMessage(Message message) throws MessagingException, IOException {
		if (message.isMimeType("text/plain")) {
			return message.getContent().toString();
		} else if (message.isMimeType("multipart/*")) {
			return getTextFromMimeMultipart((MimeMultipart)message.getContent());
		}
		return "";
	}

	private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
		for (int i = 0; i < mimeMultipart.getCount(); i++) {
			BodyPart bodyPart = mimeMultipart.getBodyPart(i);
			if (bodyPart.isMimeType("text/plain")) {
				return bodyPart.getContent().toString(); // 첫 번째 plain text 파트만 반환
			} else if (bodyPart.getContent() instanceof MimeMultipart) {
				// 재귀적으로 탐색 중첩 구조 대응
				String result = getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
				if (!result.isEmpty())
					return result;
			}
		}
		return "";
	}

	private String getSubjectSafe(Message message) {
		try {
			return message.getSubject();
		} catch (MessagingException e) {
			return "Unknown Subject";
		}
	}

}
