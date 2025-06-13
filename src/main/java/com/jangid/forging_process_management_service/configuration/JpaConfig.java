package com.jangid.forging_process_management_service.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPA auditing configuration
    // This enables @CreatedDate and @LastModifiedDate annotations
    // to automatically populate timestamp fields
} 