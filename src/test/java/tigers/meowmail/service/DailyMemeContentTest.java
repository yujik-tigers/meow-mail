package tigers.meowmail.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class DailyMemeContentTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("컨텐츠 서버의 JSON 응답을 DailyMemeContent로 매핑한다")
	void mapsContentAnalysisResponse() throws Exception {
		String response = """
			{
			  "image_url": "https://i.redd.it/djk07fn9ooyg1.jpeg",
			  "meme_text": "The new cat taught the old cat to eat like this.",
			  "expressions": "teach someone to (do something)",
			  "translation": "~에게 (무엇을) 하도록/하는 법을 가르치다",
			  "background": null,
			  "author": "mikelbv",
			  "source": "reddit-Catmemes"
			}
			""";

		DailyMemeContent content = objectMapper.readValue(response, DailyMemeContent.class);

		assertThat(content.imageUrl()).isEqualTo("https://i.redd.it/djk07fn9ooyg1.jpeg");
		assertThat(content.memeText()).isEqualTo("The new cat taught the old cat to eat like this.");
		assertThat(content.expressions()).isEqualTo("teach someone to (do something)");
		assertThat(content.translation()).isEqualTo("~에게 (무엇을) 하도록/하는 법을 가르치다");
		assertThat(content.background()).isNull();
		assertThat(content.author()).isEqualTo("mikelbv");
		assertThat(content.source()).isEqualTo("reddit-Catmemes");
	}

}
