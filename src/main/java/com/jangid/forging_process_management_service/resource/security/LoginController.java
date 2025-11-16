package com.jangid.forging_process_management_service.resource.security;

import com.jangid.forging_process_management_service.configuration.security.JwtTokenProvider;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.security.TenantContext;
import com.jangid.forging_process_management_service.entitiesRepresentation.security.LoginRequestRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.security.LoginResponseRepresentation;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.security.CustomUserDetailsService;
import com.jangid.forging_process_management_service.service.security.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

  private final AuthenticationManager authenticationManager;
  private final UserService userService;
  private final TenantService tenantService;
  private final JwtTokenProvider jwtTokenProvider;
  private final CustomUserDetailsService customUserDetailsService;

  public LoginController(AuthenticationManager authenticationManager, UserService userService, TenantService tenantService, JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
    this.authenticationManager = authenticationManager;
    this.userService = userService;
    this.tenantService = tenantService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.customUserDetailsService = customUserDetailsService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseRepresentation> login(@RequestBody LoginRequestRepresentation loginRequest) {
    // Authenticate the user with username and password
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
    );

    String tenantName = loginRequest.getTenantName();
    // Set the tenant context for the user
    TenantContext.setCurrentTenant(tenantName);

    // Generate JWT token (or any other authentication response)
    String token = userService.generateToken(authentication);

    // Extract user roles
    Set<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());

    Tenant tenant = tenantService.getTenantByTenantName(tenantName);

    // Respond with token, user details, and roles
    LoginResponseRepresentation response = new LoginResponseRepresentation(
        token, 
        authentication.getName(), 
        tenant.getId(), 
        tenantName,
        roles
    );
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    TenantContext.clear();
    return ResponseEntity.ok().build();
  }

  @PostMapping("/validate-token")
  public ResponseEntity<Map<String, Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
    try {
      // Extract token from Authorization header
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("valid", false));
      }

      String token = authHeader.substring(7); // Remove "Bearer " prefix

      // Validate the token using JwtTokenProvider
      boolean isValid = jwtTokenProvider.validateToken(token);

      if (isValid) {
        // If token is valid, also get and verify the user details
        String username = jwtTokenProvider.getUsernameFromToken(token);
        String tenantName = jwtTokenProvider.getTenantFromToken(token);

        // Set the tenant context
        TenantContext.setCurrentTenant(tenantName);

        // Additional check to ensure user still exists and is valid
        try {
          customUserDetailsService.loadUserByUsername(username + "@" + tenantName);
          return ResponseEntity.ok(Map.of("valid", true));
        } catch (Exception e) {
          return ResponseEntity.ok(Map.of("valid", false));
        }
      }

      return ResponseEntity.ok(Map.of("valid", false));

    } catch (Exception e) {
      return ResponseEntity.ok(Map.of("valid", false));
    } finally {
      // Clear tenant context
      TenantContext.clear();
    }
  }
}
