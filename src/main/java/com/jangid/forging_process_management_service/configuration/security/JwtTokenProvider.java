package com.jangid.forging_process_management_service.configuration.security;

import com.jangid.forging_process_management_service.service.security.UserService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final Key secretKey; // Store the secret key

  @Autowired
  public JwtTokenProvider(UserService userService) {
    this.secretKey = userService.getSecretKey(); // Use the secret key from UserService
  }

  // Generate JWT Token
  public String createToken(String username) {
    Claims claims = Jwts.claims().setSubject(username);
    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000); // 1 hour validity

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(secretKey) // Use the same key for signing
        .compact();
  }

  // Validate JWT Token
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(secretKey) // Use the same key for validation
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // Get username from token
  public String getUsernameFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(secretKey) // Use the same key for parsing
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  // Get tenant from token
  public String getTenantFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();

    return claims.get("tenant", String.class);
  }
}
