package tigers.meowmail.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import tigers.meowmail.config.properties.ContentProperties;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient contentWebClient(ContentProperties contentProperties) {
		HttpClient httpClient = HttpClient.create()
			.followRedirect(true)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)   // connection timeout 5s
			.responseTimeout(Duration.ofMinutes(1));               // read timeout 60s
		ExchangeStrategies strategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(contentProperties.maxBufferSize()))
			.build();

		return WebClient.builder()
			.baseUrl(contentProperties.apiBaseUrl())
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.exchangeStrategies(strategies)
			.build();
	}

}
