package tigers.meowmail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.config.properties.ContentProperties;
import tigers.meowmail.service.ContentAssetDownloader.DownloadedContentAsset;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

	private static final String DATE = "2026-05-04";
	private static final String ASSET_URL = "https://image.example/cat.jpeg";

	@TempDir
	Path tempDir;

	@Mock
	private ContentClient contentClient;

	@Mock
	private ContentAssetDownloader contentAssetDownloader;

	private DailyMemeContentStorage storage;
	private ContentService contentService;

	@BeforeEach
	void setUp() {
		storage = new DailyMemeContentStorage(
			new ObjectMapper(),
			new ContentProperties("https://content.example", "/daily-meme", tempDir.toString(), 1024 * 1024)
		);
		contentService = new ContentService(
			contentClient,
			storage,
			contentAssetDownloader,
			new AppProperties("https://meow.example", "Asia/Seoul")
		);
	}

	@Test
	@DisplayName("컨텐츠 서버 응답과 이미지 파일을 임시 디렉토리에 저장한다")
	void fetchAndSaveDailyMemeContentStoresMetadataAndAssetInTemporaryDirectory() {
		DailyMemeContent content = content();
		byte[] imageBytes = "fake-image".getBytes();
		when(contentClient.requestDailyMemeContent(DATE)).thenReturn(Optional.of(content));
		when(contentAssetDownloader.download(DATE, ASSET_URL))
			.thenReturn(Optional.of(new DownloadedContentAsset(imageBytes, ".jpg")));

		boolean saved = contentService.fetchAndSaveDailyMemeContent(DATE);

		assertThat(saved).isTrue();
		Path assetPath = tempDir.resolve(DATE + "-daily-meme.jpg");
		Path metadataPath = tempDir.resolve(DATE + "-daily-meme.json");
		assertThat(assetPath).exists().hasBinaryContent(imageBytes);
		assertThat(metadataPath).exists();
		assertThat(storage.findContent(DATE)).hasValue(content);
		assertThat(storage.hasDailyMemeContent(DATE)).isTrue();
	}

	@Test
	@DisplayName("컨텐츠 서버 응답이 없으면 이미지 다운로드 없이 저장하지 않는다")
	void fetchAndSaveDailyMemeContentReturnsFalseAndStoresNothingWhenServerReturnsNoContent() {
		when(contentClient.requestDailyMemeContent(DATE)).thenReturn(Optional.empty());

		boolean saved = contentService.fetchAndSaveDailyMemeContent(DATE);

		assertThat(saved).isFalse();
		assertThat(tempDir).isEmptyDirectory();
		verifyNoInteractions(contentAssetDownloader);
	}

	@Test
	@DisplayName("응답에 image_url이 없으면 이미지 다운로드 없이 저장하지 않는다")
	void fetchAndSaveDailyMemeContentReturnsFalseAndStoresNothingWhenImageUrlIsBlank() {
		DailyMemeContent content = new DailyMemeContent(
			" ",
			"expression",
			"translation",
			null,
			"author",
			"source"
		);
		when(contentClient.requestDailyMemeContent(DATE)).thenReturn(Optional.of(content));

		boolean saved = contentService.fetchAndSaveDailyMemeContent(DATE);

		assertThat(saved).isFalse();
		assertThat(tempDir).isEmptyDirectory();
		verifyNoInteractions(contentAssetDownloader);
	}

	@Test
	@DisplayName("다운로드한 이미지가 비어 있으면 저장하지 않는다")
	void fetchAndSaveDailyMemeContentReturnsFalseAndStoresNothingWhenDownloadedAssetIsEmpty() {
		when(contentClient.requestDailyMemeContent(DATE)).thenReturn(Optional.of(content()));
		when(contentAssetDownloader.download(DATE, ASSET_URL))
			.thenReturn(Optional.of(new DownloadedContentAsset(new byte[0], ".jpg")));

		boolean saved = contentService.fetchAndSaveDailyMemeContent(DATE);

		assertThat(saved).isFalse();
		assertThat(tempDir).isEmptyDirectory();
	}

	@Test
	@DisplayName("컨텐츠 서버 요청 중 예외가 발생하면 저장하지 않고 실패 처리한다")
	void fetchAndSaveDailyMemeContentReturnsFalseWhenContentClientThrows() {
		when(contentClient.requestDailyMemeContent(DATE)).thenThrow(new IllegalStateException("content server down"));

		boolean saved = contentService.fetchAndSaveDailyMemeContent(DATE);

		assertThat(saved).isFalse();
		assertThat(tempDir).isEmptyDirectory();
		verifyNoInteractions(contentAssetDownloader);
	}

	@Test
	@DisplayName("이미지 다운로드 중 예외가 발생하면 저장하지 않고 실패 처리한다")
	void fetchAndSaveDailyMemeContentReturnsFalseWhenAssetDownloaderThrows() {
		when(contentClient.requestDailyMemeContent(DATE)).thenReturn(Optional.of(content()));
		when(contentAssetDownloader.download(DATE, ASSET_URL)).thenThrow(new IllegalStateException("asset server down"));

		boolean saved = contentService.fetchAndSaveDailyMemeContent(DATE);

		assertThat(saved).isFalse();
		assertThat(tempDir).isEmptyDirectory();
	}

	@Test
	@DisplayName("이미 완성된 컨텐츠가 있으면 컨텐츠 서버에 다시 요청하지 않는다")
	void fetchAndSaveDailyMemeContentSkipsFetchWhenCompleteContentAlreadyExists() throws Exception {
		DailyMemeContent content = content();
		storage.save(DATE, content, new DownloadedContentAsset("existing".getBytes(), ".jpg"));

		boolean saved = contentService.fetchAndSaveDailyMemeContent(DATE);

		assertThat(saved).isTrue();
		assertThat(Files.readAllBytes(tempDir.resolve(DATE + "-daily-meme.jpg"))).isEqualTo("existing".getBytes());
		verify(contentClient, never()).requestDailyMemeContent(DATE);
		verifyNoInteractions(contentAssetDownloader);
	}

	private static DailyMemeContent content() {
		return new DailyMemeContent(
			ASSET_URL,
			"teach someone to (do something)",
			"~에게 (무엇을) 하도록/하는 법을 가르치다",
			null,
			"mikelbv",
			"reddit-Catmemes"
		);
	}

}
