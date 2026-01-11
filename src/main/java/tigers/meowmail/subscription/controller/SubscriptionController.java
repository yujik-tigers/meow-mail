package tigers.meowmail.subscription.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.subscription.service.SubscriptionConfirmResult;
import tigers.meowmail.subscription.service.SubscriptionService;

@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	@GetMapping
	public String confirm(@RequestParam("token") String rawToken, Model model) {
		SubscriptionConfirmResult result = subscriptionService.confirm(rawToken);

		model.addAttribute("result", result.getResult());
		model.addAttribute("title_en", result.getTitle_en());
		model.addAttribute("message_en", result.getMessage_en());
		model.addAttribute("message_ko", result.getMessage_ko());
		model.addAttribute("type", result.getType());

		return "subscription-confirm-result";
	}

}
