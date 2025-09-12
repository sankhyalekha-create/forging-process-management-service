package com.jangid.forging_process_management_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class DocumentConfig {

    /**
     * Configure multipart file upload settings
     */
    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
        return multipartResolver;
    }

    /**
     * Configure multipart limits
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement(
                "/tmp", // temp location
                10485760L, // max file size (10MB)
                20971520L, // max request size (20MB)
                1048576 // file size threshold (1MB)
        );
    }
}
