package tigers.meowmail.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.service.ContentAssetDownloader.DownloadedContentAsset;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

	public static final String DAILY_MEME_KEY = "daily-meme";

	private final ContentClient contentClient;
	private final DailyMemeContentStorage dailyMemeContentStorage;
	private final ContentAssetDownloader contentAssetDownloader;
	private final AppProperties appProperties;

	@Scheduled(cron = "${schedule.fetch-content-cron:0 30 7 * * *}", zone = "${app.timezone}")
	public void prepareTodayContent() {
		ZoneId zoneId = ZoneId.of(appProperties.timezone());
		LocalDate today = LocalDate.now(zoneId);
		fetchAndSaveDailyMemeContent(today.toString());
	}

	public boolean fetchAndSaveDailyMemeContent(String date) {
		if (dailyMemeContentStorage.hasDailyMemeContent(date)) {
			log.info("Daily meme content already exists for {}, skipping fetch", date);
			return true;
		}

		dailyMemeContentStorage.deleteDailyMemeContent(date);

		DailyMemeContent content;
		try {
			content = contentClient.requestDailyMemeContent(date).orElse(null);
		} catch (RuntimeException e) {
			log.error("Failed to request daily meme content for {}", date, e);
			return false;
		}
		if (!isValidContent(date, content)) {
			return false;
		}

		DownloadedContentAsset asset;
		try {
			asset = contentAssetDownloader.download(date, content.imageUrl()).orElse(null);
		} catch (RuntimeException e) {
			log.error("Failed to download daily meme content asset for {}", date, e);
			return false;
		}
		if (asset == null || asset.bytes().length == 0) {
			log.warn("Received empty daily meme content asset for {}", date);
			return false;
		}

		try {
			dailyMemeContentStorage.save(date, content, asset);
			return true;
		} catch (IOException e) {
			log.error("Failed to save daily meme content for {}", date, e);
			return false;
		}
	}

	public Optional<Path> findDailyMemeAssetPath(String date) {
		return dailyMemeContentStorage.findAssetPath(date);
	}

	public Optional<DailyMemeContent> findDailyMemeContent(String date) {
		return dailyMemeContentStorage.findContent(date);
	}

	public void deleteDailyMemeContent(String date) {
		dailyMemeContentStorage.deleteDailyMemeContent(date);
	}

	private static boolean isValidContent(String date, DailyMemeContent content) {
		if (content == null) {
			log.warn("Daily meme content is empty for {}", date);
			return false;
		}
		if (isBlank(content.imageUrl())) {
			log.warn("Daily meme content for {} has no image_url. Skipping save.", date);
			return false;
		}
		if (isBlank(content.expression())) {
			log.warn("Daily meme content for {} has no expressions. Email will use an empty value.", date);
		}
		if (isBlank(content.expressionTranslation())) {
			log.warn("Daily meme content for {} has no translation. Email will use an empty value.", date);
		}
		if (isBlank(content.author())) {
			log.warn("Daily meme content for {} has no attribution fields. Email will omit attribution.", date);
		}
		return true;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
