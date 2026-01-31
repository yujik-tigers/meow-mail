package tigers.meowmail.service;

import static tigers.meowmail.service.SubscriptionResult.ALREADY_ACTIVE;
import static tigers.meowmail.service.SubscriptionResult.CONFIRMED;
import static tigers.meowmail.service.SubscriptionResult.SUBSCRIBER_NOT_FOUND;
import static tigers.meowmail.service.SubscriptionResult.TOKEN_EXPIRED;
import static tigers.meowmail.service.SubscriptionResult.TOKEN_INVALID;
import static tigers.meowmail.service.SubscriptionResult.TOKEN_USED;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.entity.Subscriber;
import tigers.meowmail.entity.SubscriptionStatus;
import tigers.meowmail.entity.SubscriptionToken;
import tigers.meowmail.repository.SubscriptionTokenRepository;
import tigers.meowmail.util.TokenCodec;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionService {

	// IMAP: Internet Message Access Protocol
	// IMAP(143) / IMAPS(993)
	// IMAP: 메일 서버에 저장된 이메일을 클라이언트가 원격으로 조회하고 관리하기 위한 프로토콜
	// SMTP: Simple Mail Transfer Protocol
	// SMTP(25) / SMTP Submission(587) / SMTPS(465)
	// SMTP: 이메일을 한 서버에서 다른 서버로 전송하기 위한 프로토콜
	// SMTP vs SMTP Submission: SMTP는 메일 서버끼리 메일을 전달하기 위한 프로토콜, SMTP Submission은 애플리케이션이 인증을 거쳐 메일을 서버에 제출하기 위한 프로토콜

	private final SubscriptionTokenRepository subscriptionTokenRepo;
	private final Clock clock;

	public SubscriptionResult confirm(String rawToken) {
		String tokenHashHex = TokenCodec.sha256Hex(rawToken);

		SubscriptionToken token = subscriptionTokenRepo.findByTokenHashHex(tokenHashHex).orElse(null);
		if (token == null)
			return TOKEN_INVALID;

		Subscriber subscriber = token.getSubscriber();
		if (subscriber == null)
			return SUBSCRIBER_NOT_FOUND;

		Instant now = Instant.now(clock);
		if (subscriber.getStatus() == SubscriptionStatus.ACTIVE) {
			token.markUsed(now);
			return ALREADY_ACTIVE;
		}

		if (token.isUsed())   // Concurrency
			return TOKEN_USED;
		if (token.isExpired(now))
			return TOKEN_EXPIRED;

		subscriber.markActive(now);
		token.markUsed(now);

		return CONFIRMED;
	}

}
