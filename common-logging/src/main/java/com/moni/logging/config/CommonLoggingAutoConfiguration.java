package com.moni.logging.config;

import com.moni.logging.filter.AccessLogFilter;
import com.moni.logging.filter.CommonMdcFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication
public class CommonLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommonMdcFilter commonMdcFilter() {
        return new CommonMdcFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessLogFilter accessLogFilter() {
        return new AccessLogFilter();
    }
}
