package tigers.meowmail.controller.dto;

import java.util.Map;

public record ErrorResponse(
	String message,
	Map<String, String> errors
) {

}
