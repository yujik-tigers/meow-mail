package tigers.meowmail.controller.dto;

import jakarta.validation.constraints.NotBlank;
import tigers.meowmail.controller.validator.ValidEmail;
import tigers.meowmail.controller.validator.ValidTime;

public record SubscriptionRequest(
	@ValidEmail
	@NotBlank(message = "Email is required")
	String email,

	@ValidTime
	@NotBlank(message = "Time is required")
	String time) {

}
