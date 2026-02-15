package tigers.meowmail.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.ImageProperties;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final WebClient imageWebClient;
	private final ImageProperties imageProperties;

	private record ImageData(byte[] bytes, String extension) {

	}

	// 매일 22:00 KST에 다음 날 사진을 미리 받아 둠
	@Scheduled(cron = "0 25 21 * * *", zone = "Asia/Seoul")
	public void requestImageOfNextDay() {
		LocalDate tomorrow = LocalDate.now(KST).plusDays(1);
		fetchAndSaveImage(tomorrow.toString());
	}

	public void fetchAndSaveImage(String date) {
		log.info("Requesting image for {}", date);

		ImageData imageData = imageWebClient.post()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("created_at", date))
			.exchangeToMono(response -> {
				String extension = toExtension(response.headers().contentType().orElse(null));
				log.info("Image API response: status={}, content-type={}, content-length={}, extension={}",
					response.statusCode(),
					response.headers().contentType().orElse(null),
					response.headers().contentLength().orElse(-1),
					extension);
				if (response.statusCode().is2xxSuccessful()) {
					return response.bodyToMono(byte[].class)
						.map(bytes -> new ImageData(bytes, extension));
				}
				return response.bodyToMono(String.class)
					.doOnNext(body -> log.error("Image API error: status={}, body={}", response.statusCode(), body))
					.then(reactor.core.publisher.Mono.empty());
			})
			.block();

		if (imageData == null || imageData.bytes().length == 0) {
			log.warn("Received empty image for {}", date);
			return;
		}

		log.info("Received image for {}: {} bytes ({} KB)", date, imageData.bytes().length, imageData.bytes().length / 1024);

		try {
			Path storageDirectory = Paths.get(imageProperties.storagePath());
			Files.createDirectories(storageDirectory);

			Path imagePath = storageDirectory.resolve(date + imageData.extension());
			Files.write(imagePath, imageData.bytes());
			log.info("Image saved: {}", imagePath);
		} catch (IOException e) {
			log.error("Failed to save image for {}", date, e);
		}
	}

	public Optional<Path> findImagePath(String date) {
		Path storageDirectory = Paths.get(imageProperties.storagePath());
		try {
			return Files.find(storageDirectory, 1,
					(path, attr) -> path.getFileName().toString().startsWith(date))
				.findFirst();
		} catch (IOException e) {
			log.error("Failed to find image for {}", date, e);
			return Optional.empty();
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
