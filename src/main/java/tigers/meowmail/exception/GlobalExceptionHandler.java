package tigers.meowmail.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tigers.meowmail.controller.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new HashMap<>();
		exception.getBindingResult()
			.getFieldErrors()
			.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		ErrorResponse response = new ErrorResponse("Validation failed", errors);
		return ResponseEntity.badRequest().body(response);
	}

}
