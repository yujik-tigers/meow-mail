package tigers.meowmail.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DailyMemeContent(
	@JsonProperty("image_url")
	String imageUrl,
	String expressions,
	String translation,
	String background,
	String author,
	String source) {

}
