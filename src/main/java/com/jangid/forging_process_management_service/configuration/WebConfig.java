package com.jangid.forging_process_management_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${frontend.url}")
  private String frontendUrl;

  @Value("${spring.rest.template.connection-timeout:30000}")
  private int connectionTimeout;

  @Value("${spring.rest.template.read-timeout:30000}")
  private int readTimeout;

  /**
   * RestTemplate bean for making HTTP requests to external APIs
   * Used by GSP E-Way Bill services
   */
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofMillis(connectionTimeout))
        .setReadTimeout(Duration.ofMillis(readTimeout))
        .build();
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(frontendUrl, "http://www.fopmas.com", "http://fopmas.com", "http://91.108.105.97", "http://91.108.105.97:80")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization")
            .allowCredentials(true)
            .maxAge(3600); // Cache preflight requests for 1 hour
      }
    };
  }
}

