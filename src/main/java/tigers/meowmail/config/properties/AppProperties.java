package tigers.meowmail.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, String timezone, String adminEmail, List<String> adminAllowedIps) {

}
