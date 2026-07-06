package com.moni.portfolio.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    public static final String PORTFOLIO_ANALYSIS_TASK_EXECUTOR = "portfolioAnalysisTaskExecutor";

    @Bean(PORTFOLIO_ANALYSIS_TASK_EXECUTOR)
    public Executor portfolioAnalysisTaskExecutor(
            @Value("${portfolio.analysis.async.core-pool-size:4}") int corePoolSize,
            @Value("${portfolio.analysis.async.max-pool-size:16}") int maxPoolSize,
            @Value("${portfolio.analysis.async.queue-capacity:100}") int queueCapacity,
            @Value("${portfolio.analysis.async.await-termination-seconds:30}") int awaitTerminationSeconds
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("portfolio-ai-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
