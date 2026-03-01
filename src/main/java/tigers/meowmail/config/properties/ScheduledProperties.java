package tigers.meowmail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scheduled")
public record ScheduledProperties(
	String sendEmailCron,
	String fetchImageCron
) {

}
