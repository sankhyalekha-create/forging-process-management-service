package com.jangid.forging_process_management_service.configuration.security;

import com.jangid.forging_process_management_service.CustomAuthenticationFilter;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.security.UserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserService userService;
  private final TenantService tenantService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  // Inject JwtAuthenticationFilter in the constructor
  public SecurityConfig(UserService userService, TenantService tenantService, JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.userService = userService;
    this.tenantService = tenantService;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
    // Pass the UserService to the CustomAuthenticationFilter constructor
    CustomAuthenticationFilter customAuthFilter = new CustomAuthenticationFilter(userService, tenantService);
    customAuthFilter.setAuthenticationManager(authManager);
    customAuthFilter.setFilterProcessesUrl("/api/auth/login"); // Custom login endpoint

    http
        .cors()
        .and()
        .csrf().disable() // Disable CSRF for simplicity; adjust for production
        .authorizeHttpRequests(auth -> auth
<<<<<<< Updated upstream
=======
//            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // Permit all OPTIONS requests
>>>>>>> Stashed changes
            .requestMatchers("/api/auth/login").permitAll() // Permit login API without authentication
            .requestMatchers("/api/tenant/1/registerUser").permitAll() // Permit user registration API without authentication
            .requestMatchers("/api/tenants").permitAll() // Permit user registration API without authentication
            .anyRequest().authenticated() // Restrict access to other endpoints
        )
        .addFilterAt(customAuthFilter, UsernamePasswordAuthenticationFilter.class) // Add custom filter for login
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Add JWT filter before UsernamePasswordAuthenticationFilter
        .logout(logout -> logout
            .logoutUrl("/api/auth/logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Enforce stateless sessions (no session storage)
        );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // Use BCrypt for password hashing
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}
