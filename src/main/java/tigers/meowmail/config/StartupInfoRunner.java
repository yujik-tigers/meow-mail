package tigers.meowmail.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.config.properties.SubscriptionProperties;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupInfoRunner implements ApplicationRunner {

	private final AppProperties appProperties;
	private final SubscriptionProperties subscriptionProperties;

	@Override
	public void run(ApplicationArguments args) {
		String version = getClass().getPackage().getImplementationVersion();
		if (version == null) {
			version = "dev";
		}

		log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
		log.info("📮  Application Version  : {}", version);
		log.info("🌍  Timezone             : {}", appProperties.timezone());
		log.info("⏰  Default Send Time    : {}", subscriptionProperties.defaultTime());
		log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
	}
	
}
