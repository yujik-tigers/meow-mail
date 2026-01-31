package tigers.meowmail.util;

import java.io.IOException;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.MailPollingProperties.AccountProperties;
import tigers.meowmail.config.MailPollingProperties.ImapProperties;

@Slf4j
public class ImapMailReader implements AutoCloseable {

	private static final String LOG_PREFIX = "[ImapMailReader]";
	private static final String PROTOCOL = "imaps";
	private static final String FOLDER_INBOX = "INBOX";

	private Store store;
	private Folder inbox;

	public void connect(ImapProperties imap, AccountProperties account) throws MessagingException {
		Properties props = new Properties();
		props.put("mail.store.protocol", PROTOCOL);
		props.put("mail.imaps.timeout", String.valueOf(imap.timeout()));
		props.put("mail.imaps.connectiontimeout", String.valueOf(imap.connectionTimeout()));

		store = Session.getInstance(props).getStore(PROTOCOL);
		store.connect(imap.host(), account.username(), account.password());

		inbox = store.getFolder(FOLDER_INBOX);
		inbox.open(Folder.READ_WRITE);
	}

	public Message[] fetchUnreadMessages() throws MessagingException {
		return inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
	}

	public void markAsRead(Message message) throws MessagingException {
		message.setFlag(Flags.Flag.SEEN, true);
	}

	public String getSenderEmail(Message message) {
		try {
			Address[] fromAddresses = message.getFrom();
			if (fromAddresses != null && fromAddresses.length > 0) {
				Address from = fromAddresses[0];
				if (from instanceof InternetAddress internetAddress) {
					return internetAddress.getAddress();
				}
				return from.toString();
			}
		} catch (MessagingException e) {
			log.warn("{} Failed to get sender email", LOG_PREFIX, e);
		}
		return null;
	}

	public String getTextContent(Message message) throws MessagingException, IOException {
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
				return bodyPart.getContent().toString();
			} else if (bodyPart.getContent() instanceof MimeMultipart nested) {
				String result = getTextFromMimeMultipart(nested);
				if (!result.isEmpty()) {
					return result;
				}
			}
		}
		return "";
	}

	public String getSubjectSafe(Message message) {
		try {
			return message.getSubject();
		} catch (MessagingException e) {
			return "Unknown Subject";
		}
	}

	@Override
	public void close() {
		try {
			if (inbox != null && inbox.isOpen()) {
				inbox.close(false);
			}
		} catch (Exception e) { /* ignore */ }
		try {
			if (store != null) {
				store.close();
			}
		} catch (Exception e) { /* ignore */ }
	}

}
