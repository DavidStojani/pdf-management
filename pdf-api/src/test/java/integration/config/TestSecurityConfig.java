package integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityContext mockSecurityContext() {
        return SecurityContextHolder.createEmptyContext();
    }
}
