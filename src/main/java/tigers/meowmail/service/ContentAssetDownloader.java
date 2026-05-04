package tigers.meowmail.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentAssetDownloader {

	public record DownloadedContentAsset(byte[] bytes, String extension) {

	}

	private final WebClient contentWebClient;

	public Optional<DownloadedContentAsset> download(String date, String assetUrl) {
		URI uri;
		try {
			uri = new URI(assetUrl);
		} catch (IllegalArgumentException | URISyntaxException e) {
			log.warn("Invalid content asset URL for {}: {}", date, assetUrl, e);
			return Optional.empty();
		}

		return contentWebClient.get()
			.uri(uri)
			.exchangeToMono(response -> {
				String extension = toExtension(response.headers().contentType().orElse(null), assetUrl);
				log.info("Content asset download response ({}): status={}, content-type={}, content-length={}, extension={}",
					date,
					response.statusCode(),
					response.headers().contentType().orElse(null),
					response.headers().contentLength().orElse(-1),
					extension);
				if (response.statusCode().is2xxSuccessful()) {
					return response.bodyToMono(byte[].class)
						.map(bytes -> Optional.of(new DownloadedContentAsset(bytes, extension)));
				}
				return response.bodyToMono(String.class)
					.doOnNext(body -> log.warn("Content asset download error ({}): status={}, body={}", date, response.statusCode(), body))
					.thenReturn(Optional.<DownloadedContentAsset>empty());
			})
			.doOnError(e -> log.error("Content asset download failed ({}): {} - {}", date, e.getClass().getSimpleName(), e.getMessage()))
			.onErrorReturn(Optional.empty())
			.block();
	}

	private static String toExtension(MediaType contentType, String assetUrl) {
		if (contentType != null) {
			return switch (contentType.getSubtype()) {
				case "jpeg" -> ".jpg";
				case "gif" -> ".gif";
				case "webp" -> ".webp";
				default -> ".png";
			};
		}
		String lowerUrl = assetUrl.toLowerCase();
		if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
			return ".jpg";
		}
		if (lowerUrl.contains(".gif")) {
			return ".gif";
		}
		if (lowerUrl.contains(".webp")) {
			return ".webp";
		}
		return ".png";
	}

}
