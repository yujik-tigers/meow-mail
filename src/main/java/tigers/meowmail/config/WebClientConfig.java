package tigers.meowmail.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import tigers.meowmail.config.properties.ImageProperties;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient imageWebClient(ImageProperties imageProperties) {
		HttpClient httpClient = HttpClient.create()
			.followRedirect(true)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)   // 연결 타임아웃 5초
			.responseTimeout(Duration.ofMinutes(10));                // read 타임아웃 10분 (AI 이미지 생성 대기 포함)
		ExchangeStrategies strategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(imageProperties.maxBufferSize()))
			.build();

		return WebClient.builder()
			.baseUrl(imageProperties.apiBaseUrl())
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.exchangeStrategies(strategies)
			.build();
	}

}
