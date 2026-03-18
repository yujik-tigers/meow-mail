package tigers.meowmail.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

	public enum ImageType {
		QUOTES, MEME
	}

	private record ImageData(byte[] bytes, String extension) {

	}

	private final WebClient imageWebClient;
	private final ImageProperties imageProperties;
	private final AppProperties appProperties;

	// 매일 설정된 시간에 당일 필요한 사진을 미리 받아 둠
	@Scheduled(cron = "${scheduled.fetch-image-cron}", zone = "${app.timezone}")
	public void requestImageOfToday() {
		ZoneId zoneId = ZoneId.of(appProperties.timezone());
		LocalDate today = LocalDate.now(zoneId);
		fetchAndSaveImages(today.toString());
	}

	// 해당 날짜의 타입 반환: 기존 이미지가 있으면 그 타입, 없으면 날짜 시드로 결정
	public ImageType resolveType(String date) {
		List<Path> existingPaths = findImagePaths(date);
		boolean hasQuotes = existingPaths.stream()
			.anyMatch(p -> p.getFileName().toString().contains("-quotes-"));
		boolean hasMemes = existingPaths.stream()
			.anyMatch(p -> p.getFileName().toString().contains("-memes-"));

		if (hasQuotes) return ImageType.QUOTES;
		if (hasMemes) return ImageType.MEME;

		long seed = LocalDate.parse(date).toEpochDay();
		return new Random(seed).nextBoolean() ? ImageType.QUOTES : ImageType.MEME;
	}

	public void fetchAndSaveImages(String date) {
		List<Path> existingPaths = findImagePaths(date);
		boolean hasQuotes = existingPaths.stream()
			.anyMatch(p -> p.getFileName().toString().contains("-quotes-"));
		boolean hasMemes = existingPaths.stream()
			.anyMatch(p -> p.getFileName().toString().contains("-memes-"));

		ImageType type;
		if (hasQuotes) {
			type = ImageType.QUOTES;
			log.info("Quotes images already exist for {}, continuing with QUOTES type", date);
		} else if (hasMemes) {
			type = ImageType.MEME;
			log.info("Memes images already exist for {}, continuing with MEMES type", date);
		} else {
			// 날짜를 시드로 사용 → 같은 날짜는 항상 같은 타입
//			long seed = LocalDate.parse(date).toEpochDay();
//			type = new Random(seed).nextBoolean() ? ImageType.QUOTES : ImageType.MEME;
//			log.info("Date-seeded type selection for {}: {}", date, type);
			type = ImageType.QUOTES;
			log.info("No images found for {}, defaulting to QUOTES type", date);
		}

		fetchAndSaveImages(date, type);
	}

	public void fetchAndSaveImages(String date, ImageType type) {
		if (type == ImageType.QUOTES) {
			log.info("Requesting QUOTES images for {} (eng, kor, none)", date);
			fetchAndSaveImage(date, "quotes-eng", Map.of("created_at", date, "language", "eng"));
			fetchAndSaveImage(date, "quotes-kor", Map.of("created_at", date, "language", "kor"));
			fetchAndSaveImage(date, "quotes-none", Map.of("created_at", date, "language", "none"));
		} else {
			// 밈은 기본적으로 한국어(kor)만 fetch. eng/none은 관리자가 개별 요청 시 추가
			log.info("Requesting MEMES image for {} (kor only by default)", date);
			fetchAndSaveImage(date, "memes-kor", Map.of("created_at", date, "language", "kor"));
		}
	}

	// 관리자 페이지에서 특정 variant를 개별 요청할 때 사용
	public void fetchAndSaveImageByKey(String date, String key) {
		if (key.startsWith("quotes-")) {
			String lang = key.substring("quotes-".length());
			fetchAndSaveImage(date, key, Map.of("created_at", date, "language", lang));
		} else if (key.startsWith("memes-")) {
			String lang = key.substring("memes-".length());
			fetchAndSaveImage(date, key, Map.of("created_at", date, "language", lang));
		} else {
			log.warn("Unknown image key: {}", key);
		}
	}

	private void fetchAndSaveImage(String date, String key, Map<String, String> requestBody) {
		Path storageDirectory = Paths.get(imageProperties.storagePath());
		try {
			if (Files.exists(storageDirectory)) {
				boolean exists;
				try (Stream<Path> stream = Files.list(storageDirectory)) {
					exists = stream.anyMatch(path -> path.getFileName().toString().startsWith(date + "-" + key + "."));
				}
				if (exists) {
					log.info("Image already exists for {} ({}), skipping fetch", date, key);
					return;
				}
			}
		} catch (IOException e) {
			log.warn("Failed to check existing images for {} ({}), proceeding with fetch", date, key, e);
		}

		String apiPath = key.startsWith("quotes-")
			? imageProperties.fetchQuotesPath()
			: imageProperties.fetchMemesPath();

		log.info("Requesting {} image for {} via {}", key, date, apiPath);

		ImageData imageData = imageWebClient.post()
			.uri(apiPath)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(requestBody)
			.exchangeToMono(response -> {
				String extension = toExtension(response.headers().contentType().orElse(null));
				log.info("Image API response ({}): status={}, content-type={}, content-length={}, extension={}",
					key,
					response.statusCode(),
					response.headers().contentType().orElse(null),
					response.headers().contentLength().orElse(-1),
					extension);
				if (response.statusCode().is2xxSuccessful()) {
					return response.bodyToMono(byte[].class)
						.map(bytes -> new ImageData(bytes, extension));
				}
				return response.bodyToMono(String.class)
					.doOnNext(body -> log.error("Image API error ({}): status={}, body={}", key, response.statusCode(), body))
					.then(reactor.core.publisher.Mono.empty());
			})
			.doOnError(e -> log.error("Image fetch failed ({}/{}): {} - {}", date, key, e.getClass().getSimpleName(), e.getMessage()))
			.onErrorResume(e -> reactor.core.publisher.Mono.empty())
			.block();

		if (imageData == null || imageData.bytes().length == 0) {
			log.warn("Received empty {} image for {}", key, date);
			return;
		}

		log.info("Received {} image for {}: {} bytes ({} KB)", key, date, imageData.bytes().length,
			imageData.bytes().length / 1024);

		try {
			Files.createDirectories(storageDirectory);
			Path imagePath = storageDirectory.resolve(date + "-" + key + imageData.extension());
			Files.write(imagePath, imageData.bytes());
			log.info("Image saved: {}", imagePath);
		} catch (IOException e) {
			log.error("Failed to save {} image for {}", key, date, e);
		}
	}

	public void deleteImages(String date) {
		Path storageDirectory = Paths.get(imageProperties.storagePath());
		try {
			if (Files.exists(storageDirectory)) {
				List<Path> toDelete;
				try (Stream<Path> stream = Files.list(storageDirectory)) {
					toDelete = stream
						.filter(path -> path.getFileName().toString().startsWith(date + "-"))
						.toList();
				}

				toDelete.forEach(path -> {
					try {
						Files.delete(path);
						log.info("Deleted image: {}", path);
					} catch (IOException e) {
						log.warn("Failed to delete image: {}", path, e);
					}
				});

				// content 서버에도 삭제 요청 (삭제된 파일의 언어 코드 기준)
				toDelete.stream()
					.map(path -> {
						String filename = path.getFileName().toString();
						String stem = filename.substring(0, filename.lastIndexOf('.'));
						return stem.substring(stem.lastIndexOf('-') + 1);
					})
					.distinct()
					.forEach(langCode -> deleteFromContentServer(date, langCode));
			}
		} catch (IOException e) {
			log.warn("Failed to list images for deletion for {}", date, e);
		}
	}

	public void deleteImageByKey(String date, String key) {
		Path storageDirectory = Paths.get(imageProperties.storagePath());
		try {
			if (Files.exists(storageDirectory)) {
				List<Path> toDelete;
				try (Stream<Path> stream = Files.list(storageDirectory)) {
					toDelete = stream
						.filter(path -> path.getFileName().toString().startsWith(date + "-" + key + "."))
						.toList();
				}
				toDelete.forEach(path -> {
					try {
						Files.delete(path);
						log.info("Deleted image: {}", path);
					} catch (IOException e) {
						log.warn("Failed to delete image: {}", path, e);
					}
				});
			}
		} catch (IOException e) {
			log.warn("Failed to list images for deletion for {} ({})", date, key, e);
		}

		// content 서버에도 삭제 요청
		String langCode = key.substring(key.lastIndexOf('-') + 1);
		deleteFromContentServer(date, langCode);
	}

	private void deleteFromContentServer(String date, String languageCode) {
		try {
			imageWebClient.delete()
				.uri(imageProperties.deletePath() + "/" + date + "/" + languageCode)
				.exchangeToMono(response -> {
					log.info("Content server DELETE {}/{}: status={}", date, languageCode, response.statusCode());
					return response.releaseBody();
				})
				.block();
		} catch (Exception e) {
			log.warn("Failed to delete from content server for {}/{}", date, languageCode, e);
		}
	}

	public List<Path> findImagePaths(String date) {
		Path storageDirectory = Paths.get(imageProperties.storagePath());
		if (!Files.exists(storageDirectory)) {
			return List.of();
		}
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
