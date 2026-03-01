package tigers.meowmail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "subscription")
public record SubscriptionProperties(
	String defaultTime
) {

}
