package tigers.meowmail.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DailyMemeContent(
	@JsonProperty("image_url")
	String imageUrl,
	@JsonProperty("meme_text") String memeText,
	String expressions,
	String translation,
	String background,
	String author,
	String source) {

}
