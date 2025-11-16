package com.jangid.forging_process_management_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.security.TenantContext;
import com.jangid.forging_process_management_service.entitiesRepresentation.security.LoginResponseRepresentation;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.security.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private final UserService userService;
  private final TenantService tenantService;

  public CustomAuthenticationFilter(UserService userService, TenantService tenantService) {
    this.userService = userService;
    this.tenantService = tenantService;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException {

    String username = null;
    String password = null;
    String tenantName = null;

    // Parse JSON payload
    if (request.getContentType() != null && request.getContentType().contains("application/json")) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> requestBody = objectMapper.readValue(request.getInputStream(), Map.class);

        username = requestBody.get("username");
        password = requestBody.get("password");
        tenantName = requestBody.get("tenantName");

      } catch (IOException e) {
        throw new RuntimeException("Failed to parse authentication request body", e);
      }
    } else {
      username = request.getParameter("username");
      password = request.getParameter("password");
      tenantName = request.getParameter("tenantName");
    }

    try{
      // Handle tenant context
      if (tenantName != null) {
        TenantContext.setCurrentTenant(tenantName);
        username = username + "@" + tenantName;
      }

      // Create authentication token
      UsernamePasswordAuthenticationToken authRequest =
          new UsernamePasswordAuthenticationToken(username, password);
      setDetails(request, authRequest);

      Authentication authentication = this.getAuthenticationManager().authenticate(authRequest);

      // Handle successful authentication
      successfulAuthentication(request, response, null, authentication);
      return authentication;
    } catch (ServletException | IOException e) {
      throw new RuntimeException("Failed to parse authentication request body", e);
    }
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain chain, Authentication authResult) throws IOException, ServletException {
    String token = userService.generateToken(authResult);

    String tenantName = TenantContext.getCurrentTenant();
    Tenant tenant = tenantService.getTenantByTenantName(tenantName);

    // Extract user roles
    Set<String> roles = authResult.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());

    // Prepare the response body with roles
    LoginResponseRepresentation responseRepresentation = new LoginResponseRepresentation(
        token,
        authResult.getName(), 
        tenant.getId(), 
        tenantName,
        roles
    );

    // Send the token as a JSON response
    response.setContentType("application/json");
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.writeValue(response.getWriter(), responseRepresentation);

    // Proceed to the next filter in the chain if necessary
    if (chain != null) {
      chain.doFilter(request, response);  // Continue with the filter chain
    }
  }
}
