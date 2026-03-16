package tigers.meowmail.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.service.AdminAuthService;
import tigers.meowmail.service.EmailService;

@Controller
@RequestMapping("/admin/login")
@RequiredArgsConstructor
public class AdminLoginController {

	private final AdminAuthService adminAuthService;
	private final EmailService emailService;
	private final AppProperties appProperties;

	@GetMapping
	public String showLoginPage(HttpSession session) {
		if (Boolean.TRUE.equals(session.getAttribute(AdminAuthService.SESSION_ATTR))) {
			return "redirect:/admin";
		}
		return "view-admin-login";
	}

	@ResponseBody
	@PostMapping("/send-code")
	public ResponseEntity<Void> sendCode() {
		String code = adminAuthService.generateOtp();
		emailService.sendAdminOtpEmail(appProperties.adminEmail(), code);
		return ResponseEntity.ok().build();
	}

	@ResponseBody
	@PostMapping("/verify")
	public ResponseEntity<Void> verify(@RequestParam String code, HttpSession session) {
		if (adminAuthService.verifyOtp(code)) {
			session.setAttribute(AdminAuthService.SESSION_ATTR, true);
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/admin/login";
	}

}
