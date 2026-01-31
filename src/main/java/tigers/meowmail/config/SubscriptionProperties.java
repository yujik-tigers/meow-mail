package tigers.meowmail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "subscription")
public record SubscriptionProperties(String baseUrl, TokenProperties token) {

	public record TokenProperties(long expiryHours) {}

}
