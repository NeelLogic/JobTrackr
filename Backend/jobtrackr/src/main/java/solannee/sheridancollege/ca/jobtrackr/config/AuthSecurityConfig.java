package solannee.sheridancollege.ca.jobtrackr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

@Configuration
public class AuthSecurityConfig {

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom authSecureRandom() {
        return new SecureRandom();
    }
}
