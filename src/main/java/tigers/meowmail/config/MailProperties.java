package tigers.meowmail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.mail")
public record MailProperties(
	String username,
	String password
) {

}
