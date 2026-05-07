package tigers.meowmail.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test-mail")
public record TestMailProperties(String recipient, List<String> recipients, boolean exitAfterSend) {

}
