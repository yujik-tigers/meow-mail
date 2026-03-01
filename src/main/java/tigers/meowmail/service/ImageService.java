package tigers.meowmail.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.config.properties.ImageProperties;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

	private final WebClient imageWebClient;
	private final ImageProperties imageProperties;
	private final AppProperties appProperties;

	private record ImageData(byte[] bytes, String extension) {

	}

	// 매일 6:00에 당일 필요한 사진을 미리 받아 둠
	@Scheduled(cron = "0 0 6 * * *", zone = "${app.timezone}")
	public void requestImageOfNextDay() {
		ZoneId zoneId = ZoneId.of(appProperties.timezone());
		LocalDate today = LocalDate.now(zoneId);
		fetchAndSaveImages(today.toString());
	}

	public void fetchAndSaveImages(String date) {
		log.info("Requesting images for {} (eng, kor, none)", date);
		fetchAndSaveImageForLanguage(date, "eng");
		fetchAndSaveImageForLanguage(date, "kor");
		fetchAndSaveImageForLanguage(date, "none");
	}

	private void fetchAndSaveImageForLanguage(String date, String language) {
		log.info("Requesting {} image for {}", language, date);

		ImageData imageData = imageWebClient.post()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("created_at", date, "language", language))
			.exchangeToMono(response -> {
				String extension = toExtension(response.headers().contentType().orElse(null));
				log.info("Image API response ({}): status={}, content-type={}, content-length={}, extension={}",
					language,
					response.statusCode(),
					response.headers().contentType().orElse(null),
					response.headers().contentLength().orElse(-1),
					extension);
				if (response.statusCode().is2xxSuccessful()) {
					return response.bodyToMono(byte[].class)
						.map(bytes -> new ImageData(bytes, extension));
				}
				return response.bodyToMono(String.class)
					.doOnNext(body -> log.error("Image API error ({}): status={}, body={}", language, response.statusCode(), body))
					.then(reactor.core.publisher.Mono.empty());
			})
			.block();

		if (imageData == null || imageData.bytes().length == 0) {
			log.warn("Received empty {} image for {}", language, date);
			return;
		}

		log.info("Received {} image for {}: {} bytes ({} KB)", language, date, imageData.bytes().length, imageData.bytes().length / 1024);

		try {
			Path storageDirectory = Paths.get(imageProperties.storagePath());
			Files.createDirectories(storageDirectory);

			Path imagePath = storageDirectory.resolve(date + "-" + language + imageData.extension());
			Files.write(imagePath, imageData.bytes());
			log.info("Image saved: {}", imagePath);
		} catch (IOException e) {
			log.error("Failed to save {} image for {}", language, date, e);
		}
	}

	public List<Path> findImagePaths(String date) {
		Path storageDirectory = Paths.get(imageProperties.storagePath());
		try {
			return Files.find(storageDirectory, 1,
					(path, attr) -> path.getFileName().toString().startsWith(date))
				.toList();
		} catch (IOException e) {
			log.error("Failed to find images for {}", date, e);
			return List.of();
		}
	}

	private static String toExtension(MediaType contentType) {
		if (contentType == null) {
			return ".png";
		}
		return switch (contentType.getSubtype()) {
			case "jpeg" -> ".jpg";
			case "gif" -> ".gif";
			case "webp" -> ".webp";
			default -> ".png";
		};
	}

}
