package tigers.meowmail.controller.dto;

import jakarta.validation.constraints.NotBlank;
import tigers.meowmail.controller.validator.ValidEmail;

public record SubscriptionRequest(
	@ValidEmail
	@NotBlank(message = "Email is required")
	String email
) {
}
