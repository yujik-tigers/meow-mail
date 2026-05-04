package tigers.meowmail.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.properties.ContentProperties;
import tigers.meowmail.service.ContentAssetDownloader.DownloadedContentAsset;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyMemeContentStorage {

	private final ObjectMapper objectMapper;
	private final ContentProperties contentProperties;

	public void save(String date, DailyMemeContent content, DownloadedContentAsset asset) throws IOException {
		Path storageDirectory = storageDirectory();
		Files.createDirectories(storageDirectory);
		Path assetPath = storageDirectory.resolve(date + "-" + ContentService.DAILY_MEME_KEY + asset.extension());
		Path metadataPath = metadataPath(date);
		try {
			Files.write(assetPath, asset.bytes());
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), content);
			log.info("Daily meme content saved: asset={}, metadata={}", assetPath, metadataPath);
		} catch (IOException e) {
			deleteIfExists(assetPath);
			deleteIfExists(metadataPath);
			throw e;
		}
	}

	public Optional<Path> findAssetPath(String date) {
		Path storageDirectory = storageDirectory();
		if (!Files.exists(storageDirectory)) {
			return Optional.empty();
		}
		try (Stream<Path> stream = Files.list(storageDirectory)) {
			return stream
				.filter(path -> path.getFileName().toString().startsWith(date + "-" + ContentService.DAILY_MEME_KEY + "."))
				.filter(path -> !path.getFileName().toString().endsWith(".json"))
				.findFirst();
		} catch (IOException e) {
			log.error("Failed to find daily meme content asset for {}", date, e);
			return Optional.empty();
		}
	}

	public Optional<DailyMemeContent> findContent(String date) {
		Path metadataPath = metadataPath(date);
		if (!Files.exists(metadataPath)) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(metadataPath.toFile(), DailyMemeContent.class));
		} catch (IOException e) {
			log.error("Failed to read daily meme content metadata for {}", date, e);
			return Optional.empty();
		}
	}

	public boolean hasDailyMemeContent(String date) {
		return findAssetPath(date).isPresent() && findContent(date).isPresent();
	}

	public void deleteDailyMemeContent(String date) {
		deleteLocalFiles(date, path -> path.getFileName().toString().startsWith(date + "-" + ContentService.DAILY_MEME_KEY));
	}

	private void deleteLocalFiles(String date, Predicate<Path> predicate) {
		Path storageDirectory = storageDirectory();
		try {
			if (Files.exists(storageDirectory)) {
				List<Path> toDelete;
				try (Stream<Path> stream = Files.list(storageDirectory)) {
					toDelete = stream
						.filter(predicate)
						.toList();
				}

				toDelete.forEach(path -> {
					try {
						Files.delete(path);
						log.info("Deleted daily meme content file: {}", path);
					} catch (IOException e) {
						log.warn("Failed to delete daily meme content file: {}", path, e);
					}
				});
			}
		} catch (IOException e) {
			log.warn("Failed to list daily meme content files for deletion for {}", date, e);
		}
	}

	private Path storageDirectory() {
		return Paths.get(contentProperties.storagePath());
	}

	private Path metadataPath(String date) {
		return storageDirectory().resolve(date + "-" + ContentService.DAILY_MEME_KEY + ".json");
	}

	private static void deleteIfExists(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.warn("Failed to clean up partial daily meme content file: {}", path, e);
		}
	}

}
