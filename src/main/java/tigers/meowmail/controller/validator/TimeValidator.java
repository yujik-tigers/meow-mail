package tigers.meowmail.controller.validator;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TimeValidator implements ConstraintValidator<ValidTime, String> {

	private static final String TIME_REGEX = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";
	private static final Pattern TIME_PATTERN = Pattern.compile(TIME_REGEX);

	@Override
	public boolean isValid(String time, ConstraintValidatorContext context) {
		if (time == null || time.isBlank()) {
			return false;
		}
		return TIME_PATTERN.matcher(time).matches();
	}

}
