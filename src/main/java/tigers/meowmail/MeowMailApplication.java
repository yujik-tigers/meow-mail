package tigers.meowmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeowMailApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeowMailApplication.class, args);
	}

}
