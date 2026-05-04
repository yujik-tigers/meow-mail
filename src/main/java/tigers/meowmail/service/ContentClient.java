package tigers.meowmail.service;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.properties.ContentProperties;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentClient {

	private final WebClient contentWebClient;
	private final ContentProperties contentProperties;

	public Optional<DailyMemeContent> requestDailyMemeContent(String date) {
		return contentWebClient.get()
			.uri(uriBuilder -> uriBuilder
				.path(contentProperties.dailyMemePath())
				.queryParam("date", date)
				.build())
			.exchangeToMono(response -> {
				log.info("Content API response: status={}, content-type={}",
					response.statusCode(),
					response.headers().contentType().orElse(null));
				if (response.statusCode().is2xxSuccessful()) {
					return response.bodyToMono(DailyMemeContent.class)
						.map(Optional::of);
				}
				return response.bodyToMono(String.class)
					.doOnNext(body -> log.error("Content API error: status={}, body={}", response.statusCode(), body))
					.thenReturn(Optional.<DailyMemeContent>empty());
			})
			.doOnError(e -> log.error("Content fetch failed: {} - {}", e.getClass().getSimpleName(), e.getMessage()))
			.onErrorReturn(Optional.empty())
			.block();
	}

}
