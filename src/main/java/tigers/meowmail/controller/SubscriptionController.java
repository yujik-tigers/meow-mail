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
	private static final String VIEW_UNSUBSCRIBE_CONFIRM = "view-unsubscribe-confirm";
	private static final String VIEW_VERIFY_RESULT = "view-verify-result";

	@GetMapping("/subscribe")
	public String showSubscriptionForm() {
		return VIEW_SUBSCRIBE;
	}

	@GetMapping("/resubscribe")
	public String showResubscriptionForm(@RequestParam String token, Model model) {
		jwtProvider.validateToken(token, SUBSCRIPTION);
		String email = jwtProvider.getEmailFrom(token);
		model.addAttribute("email", email);
		model.addAttribute("token", token);
		model.addAttribute("currentTime", subscriptionService.getSubscriptionTime(email));
		return VIEW_RESUBSCRIBE;
	}

	@GetMapping("/unsubscribe")
	public String showUnsubscribeConfirm(@RequestParam String token, Model model) {
		jwtProvider.validateToken(token, SUBSCRIPTION);
		model.addAttribute("token", token);
		return VIEW_UNSUBSCRIBE_CONFIRM;
	}

	@DeleteMapping("/api/subscriptions")
	public String unsubscribe(@RequestParam String token, Model model) {
		UnsubscriptionResult result = subscriptionService.unsubscribe(token);
		model.addAttribute("result", result);
		return VIEW_UNSUBSCRIBE;
	}

	@GetMapping("/api/subscriptions/verify")
	public String verify(@RequestParam String token, Model model) {
		MessageResponse response = subscriptionService.verify(token);
		boolean success = response.message().startsWith("Your email has been successfully");
		model.addAttribute("success", success);
		model.addAttribute("message", response.message());
		return VIEW_VERIFY_RESULT;
	}

	@ResponseBody
	@GetMapping("/api/subscriptions/verify/status")
	public ResponseEntity<java.util.Map<String, Boolean>> getVerificationStatus(@RequestParam String email) {
		boolean verified = subscriptionService.isVerified(email);
		return ResponseEntity.ok(java.util.Map.of("verified", verified));
	}

	// TODO: 동일한 출발지에서 계속 요청을 보내는 경우 횟수 제한
	@ResponseBody
	@PostMapping("/api/subscriptions/verify")
	public ResponseEntity<MessageResponse> sendVerificationEmail(@Valid @RequestBody SubscriptionRequest request) {
		MessageResponse response = subscriptionService.sendVerificationEmail(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
