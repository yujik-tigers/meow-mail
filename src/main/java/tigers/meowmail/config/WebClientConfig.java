package tigers.meowmail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;
import tigers.meowmail.config.properties.ImageProperties;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient imageWebClient(ImageProperties imageProperties) {
		HttpClient httpClient = HttpClient.create().followRedirect(true);
		ExchangeStrategies strategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(imageProperties.maxBufferSize()))
			.build();

		return WebClient.builder()
			.baseUrl(imageProperties.apiUrl())
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.exchangeStrategies(strategies)
			.build();
	}

}
