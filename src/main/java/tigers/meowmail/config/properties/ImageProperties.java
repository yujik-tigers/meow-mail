package tigers.meowmail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image")
public record ImageProperties(
	String apiBaseUrl,
	String fetchQuotesPath,
	String fetchMemesPath,
	String deletePath,
	String storagePath,
	int maxBufferSize) {

}
