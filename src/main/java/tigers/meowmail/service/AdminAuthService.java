package tigers.meowmail.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

	public static final String SESSION_ATTR = "adminAuthenticated";

	private static final Duration OTP_EXPIRY = Duration.ofHours(1);

	private record OtpEntry(String code, Instant expiresAt) {
		boolean isExpired() {
			return Instant.now().isAfter(expiresAt);
		}

		boolean matches(String input) {
			return !isExpired() && code.equals(input);
		}
	}

	private final AtomicReference<OtpEntry> pendingOtp = new AtomicReference<>();

	public String generateOtp() {
		String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
		pendingOtp.set(new OtpEntry(code, Instant.now().plus(OTP_EXPIRY)));
		return code;
	}

	public boolean verifyOtp(String input) {
		OtpEntry entry = pendingOtp.get();
		if (entry == null || !entry.matches(input)) {
			return false;
		}
		pendingOtp.set(null);
		return true;
	}

}
