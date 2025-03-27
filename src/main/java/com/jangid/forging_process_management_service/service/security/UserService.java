package com.jangid.forging_process_management_service.service.security;

import com.jangid.forging_process_management_service.entities.security.TenantContext;
import com.jangid.forging_process_management_service.entities.security.Usr;
import com.jangid.forging_process_management_service.repositories.security.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

  // JWT secret key (you can load it from application.properties or env variables)
  private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

  // Token expiration time (e.g., 1 hour)
  private final long expirationTimeMillis = 86400000; // 1 hour

  private final PasswordEncoder passwordEncoder;

  @Autowired
  private UserRepository userRepository;

  public UserService() {
    this.passwordEncoder = new BCryptPasswordEncoder(); // Instantiate the BCryptPasswordEncoder
  }

  public Key getSecretKey() {
    return secretKey;
  }


  /**
   * Generates a JWT token for the authenticated user.
   *
   * @param authentication Spring Security authentication object
   * @return Generated JWT token as a String
   */
  public String generateToken(Authentication authentication) {
    String username = authentication.getName();
    Set<String> roles = authentication.getAuthorities().stream()
        .map(grantedAuthority -> grantedAuthority.getAuthority())
        .collect(Collectors.toSet());

    String tenant = TenantContext.getCurrentTenant();

    // Current timestamp
    Date now = new Date();

    // Token generation
    return Jwts.builder()
        .setSubject(username) // Username as subject
        .claim("roles", roles) // Roles claim
        .claim("tenant", tenant) // Tenant claim
        .setIssuedAt(now) // Issued at
        .setExpiration(new Date(now.getTime() + expirationTimeMillis)) // Expiration
        .signWith(secretKey) // Signing key and algorithm
        .compact();
  }

  public void registerUser(String rawPassword) {
    // Hash the plain text password using BCrypt
    String hashedPassword = passwordEncoder.encode(rawPassword);

    // Save the hashed password in the database
    saveUserToDatabase(hashedPassword);
  }

  public boolean authenticateUser(String rawPassword, String storedHashedPassword) {
    // Compare the raw password with the stored hashed password
    return passwordEncoder.matches(rawPassword, storedHashedPassword);
  }

  private void saveUserToDatabase(String hashedPassword) {
    // Save the user with the hashed password in the database
    // For example, assuming you have a User entity and a UserRepository
    Usr user = new Usr();
    user.setPassword(hashedPassword);
    userRepository.save(user); // Save the user in the database
  }
}
