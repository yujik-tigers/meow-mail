package tigers.meowmail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "content")
public record ContentProperties(
	String apiBaseUrl,
	String dailyMemePath,
	String storagePath,
	int maxBufferSize) {

}
