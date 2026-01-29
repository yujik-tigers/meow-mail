package tigers.meowmail.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TokenCodec {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final HexFormat HEX = HexFormat.of();
	private static final String ALGORITHM = "SHA-256";

	public static String newRawTokenUrlSafe() {
		byte[] buf = new byte[32]; // 32 bytes = 256-bit
		SECURE_RANDOM.nextBytes(buf);
		return HEX.formatHex(buf); // 64 chars
	}

	public static String sha256Hex(String rawToken) {
		if (rawToken == null)
			return null;

		try {
			MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
			byte[] dig = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HEX.formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(ALGORITHM + " algorithm not found", e);
		}
	}

}
