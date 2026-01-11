package tigers.meowmail.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TokenCodec {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final HexFormat HEX = HexFormat.of();

	public static String newRawTokenUrlSafe() {
		byte[] buf = new byte[32]; // 32 bytes = 256-bit
		SECURE_RANDOM.nextBytes(buf);
		return HEX.formatHex(buf); // URL-safe 형태로 쓰기 위해 hex 사용 (길이는 64 chars)
	}

	public static String sha256Hex(String rawToken) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] dig = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HEX.formatHex(dig);
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

}
