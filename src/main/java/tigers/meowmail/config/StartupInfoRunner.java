package tigers.meowmail.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tigers.meowmail.config.properties.AppProperties;
import tigers.meowmail.config.properties.ScheduleProperties;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupInfoRunner implements ApplicationRunner {

	private final AppProperties appProperties;
	private final ScheduleProperties scheduleProperties;

	@Override
	public void run(@NonNull ApplicationArguments args) {
		String version = getClass().getPackage().getImplementationVersion();
		if (version == null) {
			version = "unknown";
		}

		log.info("-----------------------------------------");
		log.info(" Application Version    : {}", version);
		log.info(" Timezone               : {}", appProperties.timezone());
		log.info(" Email Send Schedule    : {}", scheduleProperties.sendEmailCron());
		log.info(" Content Fetch Schedule : {}", scheduleProperties.fetchContentCron());
		log.info("-----------------------------------------");
	}

}
