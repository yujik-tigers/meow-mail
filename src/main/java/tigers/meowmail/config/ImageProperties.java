package tigers.meowmail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image")
public record ImageProperties(String apiUrl, String storagePath) {

}
