package com.moni.common.JpaAuditing;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// TODO: 미사용 코드. 각 서비스가 직접 JpaConfig에서 @EnableJpaAuditing +
//  AuditorAwareImpl(com.moni.common.security) 참고
@Configuration
@EnableJpaAuditing(auditorAwareRef = "awareUserAudit")
public class JpaAuditingConfig {
}
