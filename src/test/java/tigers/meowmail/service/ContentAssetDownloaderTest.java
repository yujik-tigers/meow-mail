package tigers.meowmail.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tigers.meowmail.service.ContentAssetDownloader.DownloadedContentAsset;

class ContentAssetDownloaderTest {

	private static final String DATE = "2026-05-04";
	private static final String ASSET_URL = "https://image.example/cat.jpeg";

	@Test
	@DisplayName("이미지 URL에서 파일을 다운로드하고 Content-Type 기준 확장자를 결정한다")
	void downloadReturnsAssetWithExtensionFromContentType() {
		byte[] imageBytes = "fake-image".getBytes();
		AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
		ContentAssetDownloader downloader = new ContentAssetDownloader(WebClient.builder()
			.exchangeFunction(request -> {
				capturedRequest.set(request);
				return Mono.just(ClientResponse.create(HttpStatus.OK)
					.header("Content-Type", MediaType.IMAGE_JPEG_VALUE)
					.body(Flux.just(new DefaultDataBufferFactory().wrap(imageBytes)))
					.build());
			})
			.build());

		Optional<DownloadedContentAsset> asset = downloader.download(DATE, ASSET_URL);

		ClientRequest request = capturedRequest.get();
		assertThat(request.method()).isEqualTo(HttpMethod.GET);
		assertThat(request.url().toString()).isEqualTo(ASSET_URL);
		assertThat(asset).hasValueSatisfying(downloadedAsset -> {
			assertThat(downloadedAsset.bytes()).isEqualTo(imageBytes);
			assertThat(downloadedAsset.extension()).isEqualTo(".jpg");
		});
	}

	@Test
	@DisplayName("Content-Type이 없으면 이미지 URL 확장자로 저장 확장자를 결정한다")
	void downloadUsesUrlExtensionWhenContentTypeIsMissing() {
		byte[] imageBytes = "fake-webp".getBytes();
		ContentAssetDownloader downloader = new ContentAssetDownloader(WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.body(Flux.just(new DefaultDataBufferFactory().wrap(imageBytes)))
				.build()))
			.build());

		Optional<DownloadedContentAsset> asset = downloader.download(DATE, "https://image.example/cat.webp");

		assertThat(asset).hasValueSatisfying(downloadedAsset -> {
			assertThat(downloadedAsset.bytes()).isEqualTo(imageBytes);
			assertThat(downloadedAsset.extension()).isEqualTo(".webp");
		});
	}

	@Test
	@DisplayName("잘못된 이미지 URL이면 다운로드하지 않고 빈 결과를 반환한다")
	void downloadReturnsEmptyWhenAssetUrlIsInvalid() {
		ContentAssetDownloader downloader = new ContentAssetDownloader(WebClient.builder()
			.exchangeFunction(request -> Mono.error(new AssertionError("request should not be sent")))
			.build());

		Optional<DownloadedContentAsset> asset = downloader.download(DATE, "not a url");

		assertThat(asset).isEmpty();
	}

	@Test
	@DisplayName("이미지 서버가 실패 응답을 반환하면 빈 결과를 반환한다")
	void downloadReturnsEmptyWhenServerRespondsWithError() {
		ContentAssetDownloader downloader = new ContentAssetDownloader(WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND)
				.header("Content-Type", MediaType.TEXT_PLAIN_VALUE)
				.body("not found")
				.build()))
			.build());

		Optional<DownloadedContentAsset> asset = downloader.download(DATE, ASSET_URL);

		assertThat(asset).isEmpty();
	}

	@Test
	@DisplayName("이미지 다운로드 중 예외가 발생하면 빈 결과를 반환한다")
	void downloadReturnsEmptyWhenRequestFails() {
		ContentAssetDownloader downloader = new ContentAssetDownloader(WebClient.builder()
			.exchangeFunction(request -> Mono.error(new IllegalStateException("network error")))
			.build());

		Optional<DownloadedContentAsset> asset = downloader.download(DATE, ASSET_URL);

		assertThat(asset).isEmpty();
	}

}
