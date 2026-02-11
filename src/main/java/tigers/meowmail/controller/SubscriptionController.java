package tigers.meowmail.controller;

import static tigers.meowmail.util.JwtProvider.TokenType.SUBSCRIPTION;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.controller.dto.MessageResponse;
import tigers.meowmail.controller.dto.SubscriptionRequest;
import tigers.meowmail.service.SubscriptionService;
import tigers.meowmail.service.UnsubscriptionResult;
import tigers.meowmail.util.JwtProvider;

@Controller
@RequiredArgsConstructor
public class SubscriptionController {

	private final JwtProvider jwtProvider;
	private final SubscriptionService subscriptionService;

	private static final String VIEW_SUBSCRIBE = "view-subscribe";
	private static final String VIEW_RESUBSCRIBE = "view-resubscribe";
	private static final String VIEW_UNSUBSCRIBE = "view-unsubscribe";

	@GetMapping("/subscribe")
	public String showSubscriptionForm() {
		return VIEW_SUBSCRIBE;
	}

	@GetMapping("/resubscribe")
	public String showResubscriptionForm(@RequestParam String token, Model model) {
		jwtProvider.validateToken(token, SUBSCRIPTION);
		model.addAttribute("email", jwtProvider.getEmailFrom(token));
		model.addAttribute("token", token);
		return VIEW_RESUBSCRIBE;
	}

	@DeleteMapping("/api/subscriptions")
	public String unsubscribe(@RequestParam String token, Model model) {
		UnsubscriptionResult result = subscriptionService.unsubscribe(token);
		model.addAttribute("result", result);
		return VIEW_UNSUBSCRIBE;
	}

	@ResponseBody
	@PostMapping("/api/subscriptions/verify")
	public ResponseEntity<MessageResponse> sendVerificationEmail(@Valid @RequestBody SubscriptionRequest request) {
		MessageResponse response = subscriptionService.sendVerificationEmail(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@ResponseBody
	@GetMapping("/api/subscriptions/verify")
	public ResponseEntity<MessageResponse> verify(@RequestParam String token) {
		MessageResponse response = subscriptionService.verify(token);
		return ResponseEntity.ok(response);
	}

	@ResponseBody
	@GetMapping(value = "/api/subscriptions/verify/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribeVerification(@RequestParam String email) {
		return subscriptionService.openEmitter(email);
	}

	@ResponseBody
	@PostMapping("/api/subscriptions")
	public ResponseEntity<MessageResponse> subscribe(@Valid @RequestBody SubscriptionRequest request) {
		MessageResponse response = subscriptionService.subscribe(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@ResponseBody
	@PutMapping("/api/subscriptions")
	public ResponseEntity<MessageResponse> resubscribe(@Valid @RequestBody SubscriptionRequest request) {
		MessageResponse response = subscriptionService.resubscribe(request);
		return ResponseEntity.ok(response);
	}

}
