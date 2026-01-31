package tigers.meowmail.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.service.SubscriptionResult;
import tigers.meowmail.service.SubscriptionService;

@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

	private static final String VIEW_CONFIRM_RESULT = "subscription-confirm-result";

	private final SubscriptionService subscriptionService;

	@GetMapping
	public String confirm(@RequestParam String token, Model model) {
		SubscriptionResult result = subscriptionService.confirm(token);

		model.addAttribute("result", result.getResult());
		model.addAttribute("title_en", result.getTitle_en());
		model.addAttribute("message_en", result.getMessage_en());
		model.addAttribute("message_ko", result.getMessage_ko());
		model.addAttribute("type", result.getType());

		return VIEW_CONFIRM_RESULT;
	}

}
