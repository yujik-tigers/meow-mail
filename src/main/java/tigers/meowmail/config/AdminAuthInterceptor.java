package tigers.meowmail.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.service.AdminAuthService;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

	private final AppProperties appProperties;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		// 1. IP 체크 (로그인 경로 포함 모든 /admin/** 에 적용)
		String clientIp = resolveClientIp(request);
		if (!appProperties.adminAllowedIps().contains(clientIp)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		// 2. 세션 체크 (로그인 경로는 통과)
		if (request.getRequestURI().startsWith("/admin/login")) {
			return true;
		}

		HttpSession session = request.getSession(false);
		if (session != null && Boolean.TRUE.equals(session.getAttribute(AdminAuthService.SESSION_ATTR))) {
			return true;
		}
		response.sendRedirect("/admin/login");
		return false;
	}

	private String resolveClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isBlank()) {
			return xForwardedFor.split(",")[0].trim();
		}
		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isBlank()) {
			return xRealIp.trim();
		}
		return request.getRemoteAddr();
	}

}
