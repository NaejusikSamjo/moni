package com.moni.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String SYSTEM_AUDITOR = "SYSTEM";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(resolveAuditor());
    }

    private String resolveAuditor() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String userId = attributes.getRequest().getHeader(HEADER_USER_ID);

            if (userId != null && !userId.isBlank()) {
                return userId;
            }
        }

        return SYSTEM_AUDITOR;
    }
}
