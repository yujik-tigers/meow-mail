package tigers.meowmail.util;

import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Validator {

	// RFC 5322 규격을 준수하는 표준적인 이메일 정규식
	private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

	public static boolean isValidEmail(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		return EMAIL_PATTERN.matcher(email).matches();
	}

	public static boolean isValidTime(String time) {
		return time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
	}

}
