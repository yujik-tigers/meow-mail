package tigers.meowmail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// record는 모든 필드가 기본적으로 private final
// Boilerplate 제거
// 중첩 record를 사용하면 설정 파일의 계층 구조가 객체 지향적으로 코드에 그대로 투영
// Spring Boot 3.0부터는 @ConfigurationProperties가 붙은 record에 대해 생성자 주입 방식을 기본으로 사용

@ConfigurationProperties(prefix = "mail")
public record MailPollingProperties(
	AccountProperties subscribe,
	AccountProperties unsubscribe,
	PollingProperties polling,
	ImapProperties imap
) {

	public record AccountProperties(String username, String password) {

	}

	public record PollingProperties(long subscribeRate, long unsubscribeRate) {

	}

	public record ImapProperties(String host, int timeout, int connectionTimeout) {

	}

}
