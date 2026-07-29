package solannee.sheridancollege.ca.jobtrackr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JobtrackrApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobtrackrApplication.class, args);
	}

}
