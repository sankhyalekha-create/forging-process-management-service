package com.jangid.forging_process_management_service.configuration.security;

import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.entities.Tenant;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Filter that extracts tenant information from the JWT token and sets it in TenantContextHolder.
 * This ensures tenant context is derived from authentication rather than URL parameters,
 * preventing unauthorized cross-tenant access.
 */
@Slf4j
@Component
public class TenantAuthorizationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final TenantService tenantService;

  public TenantAuthorizationFilter(JwtTokenProvider jwtTokenProvider, TenantService tenantService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.tenantService = tenantService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

    try {
      String token = extractToken(request);

      if (token != null && jwtTokenProvider.validateToken(token)) {
        String tenantName = jwtTokenProvider.getTenantFromToken(token);
        Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // If token doesn't have tenant ID (legacy tokens), fetch it from database
        if (tenantId == null && tenantName != null) {
          log.warn("Legacy token detected without tenant ID. Fetching from database for tenant: {}", tenantName);
          Tenant tenant = tenantService.getTenantByTenantName(tenantName);
          tenantId = tenant.getId();
        }

        if (tenantId != null && tenantName != null) {
          // Set the tenant context for this request
          TenantContextHolder context = TenantContextHolder.builder()
            .tenantId(tenantId)
            .tenantName(tenantName)
            .username(username)
            .build();

          TenantContextHolder.setContext(context);
          
          log.debug("Set tenant context for user: {}, tenant: {} (ID: {})", username, tenantName, tenantId);
        }
      }

      filterChain.doFilter(request, response);
    } finally {
      // Always clear the context after the request to prevent thread pollution
      TenantContextHolder.clear();
    }
  }

  /**
   * Extracts JWT token from Authorization header
   */
  private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return null;
  }
}

