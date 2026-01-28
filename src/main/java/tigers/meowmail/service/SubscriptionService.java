package tigers.meowmail.service;

import static tigers.meowmail.service.SubscriptionConfirmResult.ALREADY_ACTIVE;
import static tigers.meowmail.service.SubscriptionConfirmResult.CONFIRMED;
import static tigers.meowmail.service.SubscriptionConfirmResult.SUBSCRIBER_INACTIVE;
import static tigers.meowmail.service.SubscriptionConfirmResult.SUBSCRIBER_NOT_FOUND;
import static tigers.meowmail.service.SubscriptionConfirmResult.TOKEN_EXPIRED;
import static tigers.meowmail.service.SubscriptionConfirmResult.TOKEN_INVALID;
import static tigers.meowmail.service.SubscriptionConfirmResult.TOKEN_USED;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.entity.Subscriber;
import tigers.meowmail.entity.SubscriptionStatus;
import tigers.meowmail.entity.SubscriptionToken;
import tigers.meowmail.repository.SubscriberRepository;
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
	private final SubscriberRepository subscriberRepo;
	private final Clock clock;

	public SubscriptionConfirmResult confirm(String rawToken) {
		String tokenHashHex = TokenCodec.sha256Hex(rawToken);

		Optional<SubscriptionToken> tokenOpt = subscriptionTokenRepo.findByTokenHashHex(tokenHashHex);
		if (tokenOpt.isEmpty())
			return TOKEN_INVALID;

		SubscriptionToken token = tokenOpt.get();

		Instant now = Instant.now(clock);
		if (token.isUsed())   // Concurrency
			return TOKEN_USED;
		if (token.isExpired(now))
			return TOKEN_EXPIRED;

		Optional<Subscriber> subscriberOpt = subscriberRepo.findById(token.getSubscriberId());
		if (subscriberOpt.isEmpty())
			return SUBSCRIBER_NOT_FOUND;

		Subscriber subscriber = subscriberOpt.get();

		if (subscriber.getStatus() == SubscriptionStatus.INACTIVE)
			return SUBSCRIBER_INACTIVE;
		if (subscriber.getStatus() == SubscriptionStatus.ACTIVE) {
			token.markUsed(Instant.now(clock));
			return ALREADY_ACTIVE;
		}

		subscriber.markActive(Instant.now(clock));
		token.markUsed(Instant.now(clock));

		return CONFIRMED;
	}

}
