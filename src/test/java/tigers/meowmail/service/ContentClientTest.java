package tigers.meowmail.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import tigers.meowmail.config.properties.ContentProperties;

class ContentClientTest {

	@Test
	@DisplayName("컨텐츠 서버에 GET 요청으로 date query param을 전달하고 응답을 매핑한다")
	void requestDailyMemeContentUsesGetWithDateQueryParam() {
		AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
		WebClient webClient = WebClient.builder()
			.baseUrl("https://content.example")
			.exchangeFunction(request -> {
				capturedRequest.set(request);
				return Mono.just(ClientResponse.create(HttpStatus.OK)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.body("""
						{
						  "image_url": "https://i.redd.it/djk07fn9ooyg1.jpeg",
						  "meme_text": "The new cat taught the old cat to eat like this.",
						  "expressions": "teach someone to (do something)",
						  "translation": "~에게 (무엇을) 하도록/하는 법을 가르치다",
						  "background": null,
						  "author": "mikelbv",
						  "source": "reddit-Catmemes"
						}
						""")
					.build());
			})
			.build();
		ContentClient contentClient = new ContentClient(
			webClient,
			new ContentProperties("https://content.example", "/daily-meme", "/tmp/meow-mail", 1024 * 1024)
		);

		Optional<DailyMemeContent> content = contentClient.requestDailyMemeContent("2026-05-04");

		ClientRequest request = capturedRequest.get();
		assertThat(request.method()).isEqualTo(HttpMethod.GET);
		assertThat(request.url().getPath()).isEqualTo("/daily-meme");
		assertThat(request.url().getQuery()).isEqualTo("date=2026-05-04");
		assertThat(content).hasValueSatisfying(dailyMemeContent -> {
			assertThat(dailyMemeContent.imageUrl()).isEqualTo("https://i.redd.it/djk07fn9ooyg1.jpeg");
			assertThat(dailyMemeContent.author()).isEqualTo("mikelbv");
			assertThat(dailyMemeContent.source()).isEqualTo("reddit-Catmemes");
		});
	}

	@Test
	@DisplayName("컨텐츠 서버가 실패 응답을 반환하면 빈 결과를 반환한다")
	void requestDailyMemeContentReturnsEmptyWhenServerRespondsWithError() {
		WebClient webClient = WebClient.builder()
			.baseUrl("https://content.example")
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
				.header("Content-Type", MediaType.TEXT_PLAIN_VALUE)
				.body("server error")
				.build()))
			.build();
		ContentClient contentClient = new ContentClient(
			webClient,
			new ContentProperties("https://content.example", "/daily-meme", "/tmp/meow-mail", 1024 * 1024)
		);

		Optional<DailyMemeContent> content = contentClient.requestDailyMemeContent("2026-05-04");

		assertThat(content).isEmpty();
	}

	@Test
	@DisplayName("컨텐츠 서버 요청 중 예외가 발생하면 빈 결과를 반환한다")
	void requestDailyMemeContentReturnsEmptyWhenRequestFails() {
		WebClient webClient = WebClient.builder()
			.baseUrl("https://content.example")
			.exchangeFunction(request -> Mono.error(new IllegalStateException("network error")))
			.build();
		ContentClient contentClient = new ContentClient(
			webClient,
			new ContentProperties("https://content.example", "/daily-meme", "/tmp/meow-mail", 1024 * 1024)
		);

		Optional<DailyMemeContent> content = contentClient.requestDailyMemeContent("2026-05-04");

		assertThat(content).isEmpty();
	}

}
