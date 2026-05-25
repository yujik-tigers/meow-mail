package tigers.meowmail.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DailyMemeContent(
	String type,

	@JsonProperty("image_url")
	String imageUrl,

	String content,
	@JsonProperty("content_translation")
	String contentTranslation,

	String expression,
	@JsonProperty("expression_translation")
	String expressionTranslation,

	String author)
	{

}
