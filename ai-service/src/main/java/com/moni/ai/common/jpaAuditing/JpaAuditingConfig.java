package com.moni.ai.common.jpaAuditing;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "awareUserAudit")
public class JpaAuditingConfig {
}
