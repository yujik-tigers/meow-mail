package tigers.meowmail.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "schedule")
public record ScheduleProperties(String sendEmailCron, String fetchContentCron) {

}
