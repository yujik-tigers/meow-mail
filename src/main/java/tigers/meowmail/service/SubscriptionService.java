package tigers.meowmail.service;

import static tigers.meowmail.util.JwtProvider.TokenType.SUBSCRIPTION;
import static tigers.meowmail.util.JwtProvider.TokenType.VERIFICATION;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.controller.dto.MessageResponse;
import tigers.meowmail.controller.dto.SubscriptionRequest;
import tigers.meowmail.controller.dto.VerificationResponse;
import tigers.meowmail.entity.Subscription;
import tigers.meowmail.entity.SubscriptionStatus;
import tigers.meowmail.repository.EmitterRepository;
import tigers.meowmail.repository.SubscriptionRepository;
import tigers.meowmail.util.JwtProvider;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

	// IMAP: Internet Message Access Protocol
	// IMAP(143) / IMAPS(993)
	// IMAP: 메일 서버에 저장된 이메일을 클라이언트가 원격으로 조회하고 관리하기 위한 프로토콜
	// SMTP: Simple Mail Transfer Protocol
	// SMTP(25) / SMTP Submission(587) / SMTPS(465)
	// SMTP: 이메일을 한 서버에서 다른 서버로 전송하기 위한 프로토콜
	// SMTP vs SMTP Submission: SMTP는 메일 서버끼리 메일을 전달하기 위한 프로토콜, SMTP Submission은 애플리케이션이 인증을 거쳐 메일을 서버에 제출하기 위한 프로토콜

	private final Clock clock;
	private final JwtProvider jwtProvider;
	private final EmailService emailService;
	private final EmitterRepository emitterRepository;
	private final SubscriptionRepository subscriptionRepo;

	public MessageResponse sendVerificationEmail(SubscriptionRequest request) {
		Subscription subscription = findOrCreateSubscription(request.email(), request.time());
		String token = jwtProvider.generateVerificationToken(subscription.getEmail());
		emailService.sendVerificationEmail(request.email(), token);
		return new MessageResponse("A verification email has been sent.");
	}

	// TODO: Email Crypto
	private Subscription findOrCreateSubscription(String email, String time) {
		Instant now = Instant.now(clock);
		String normalizedEmail = email.toLowerCase();
		Optional<Subscription> subscriptionOpt = subscriptionRepo.findByEmail(normalizedEmail);

		if (subscriptionOpt.isPresent()) {
			Subscription subscription = subscriptionOpt.get();
			if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
				throw new IllegalStateException("이미 구독 중인 이메일입니다.");
			}

			subscription.updateTime(time, now);
			return subscriptionRepo.save(subscription);
		}

		Subscription subscription = Subscription.builder()
			.email(normalizedEmail)
			.time(time)
			.status(SubscriptionStatus.PENDING)
			.createdAt(now)
			.updatedAt(now)
			.build();
		return subscriptionRepo.save(subscription);
	}

	public SseEmitter openEmitter(String email) {
		SseEmitter emitter = new SseEmitter(600_000L); // 10m

		emitter.onCompletion(() -> emitterRepository.delete(email));
		emitter.onTimeout(() -> emitterRepository.delete(email));

		emitterRepository.save(email, emitter);

		// 첫 연결 시 더미 데이터 전송
		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data("connect success"));
		} catch (IOException e) {
			throw new RuntimeException("SSE Connect failed", e);
		}

		return emitter;
	}

	public MessageResponse verify(String token) {
		String email = null;
		try {
			jwtProvider.validateToken(token, VERIFICATION);

			email = jwtProvider.getEmailFrom(token);
			sendSseEvent(email, "verified", "success", "Your email has been successfully verified.");
			return new MessageResponse("Your email has been successfully verified.");
		} catch (Exception e) {
			sendSseEvent(email, "verified", "fail", "Email verification failed: " + e.getMessage());
			return new MessageResponse("Email verification failed.");
		}
	}

	private void sendSseEvent(String email, String name, String status, String message) {
		emitterRepository.findByEmail(email).ifPresent(emitter -> {
			try {
				emitter.send(SseEmitter.event()
					.name(name)
					.data(new VerificationResponse(status, message)));
				emitter.complete();
			} catch (IOException e) {
				emitter.completeWithError(e);
			}
		});
	}

	public MessageResponse subscribe(SubscriptionRequest request) {
		Optional<Subscription> subscriptionOpt = subscriptionRepo.findByEmail(request.email());
		if (subscriptionOpt.isEmpty()) {
			throw new RuntimeException("No verified email information found.");
		}

		Subscription subscription = subscriptionOpt.get();
		subscription.markActive(request.time(), Instant.now(clock));
		subscriptionRepo.save(subscription);

		return new MessageResponse("Your email has been successfully subscribed.");
	}

	public String getSubscriptionTime(String email) {
		return subscriptionRepo.findByEmail(email.toLowerCase())
			.map(Subscription::getTime)
			.orElseThrow(() -> new IllegalStateException("Subscription not found"));
	}

	public MessageResponse resubscribe(SubscriptionRequest request) {
		Subscription subscription = subscriptionRepo.findByEmail(request.email())
			.orElseThrow(() -> new IllegalStateException("Subscription not found"));

		subscription.updateTime(request.time(), Instant.now(clock));

		return new MessageResponse("Resubscribed to " + subscription.getEmail());
	}

	public UnsubscriptionResult unsubscribe(String token) {
		jwtProvider.validateToken(token, SUBSCRIPTION);

		String email = jwtProvider.getEmailFrom(token);

		Optional<Subscription> subscriptionOpt = subscriptionRepo.findByEmail(email);
		if (subscriptionOpt.isEmpty()) {
			return UnsubscriptionResult.NOT_FOUND;
		}

		Subscription subscription = subscriptionOpt.get();
		if (subscription.getStatus() == SubscriptionStatus.INACTIVE) {
			return UnsubscriptionResult.ALREADY_INACTIVE;
		}

		subscription.markInactive(Instant.now(clock));
		subscriptionRepo.save(subscription);
		return UnsubscriptionResult.SUCCESS;
	}

}
