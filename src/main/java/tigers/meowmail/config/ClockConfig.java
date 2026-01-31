package tigers.meowmail.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	// Clock을 주입받아 사용하면 테스트 코드에서 Clock.fixed()를 사용해 시간을 특정 시점에 박제 가능
	// 모든 서버의 서비스가 동일한 시간대를 기준으로 동작함을 보장
	// 향후 특정 로직에서만 다른 시간대를 적용해야 하거나, 특수한 시간 계산 로직이 필요할 때 빈만 교체하거나 설정을 변경

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
