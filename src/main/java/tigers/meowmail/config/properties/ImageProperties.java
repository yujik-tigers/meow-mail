package tigers.meowmail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image")
public record ImageProperties(String apiUrl, String storagePath, int maxBufferSize) {

}
