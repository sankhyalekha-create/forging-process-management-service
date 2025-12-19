package com.jangid.forging_process_management_service.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous email sending
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring's @EnableAsync will use default thread pool
    // For production, consider customizing with @Bean TaskExecutor
}
