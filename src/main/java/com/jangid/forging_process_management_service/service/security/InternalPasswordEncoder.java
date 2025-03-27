package com.jangid.forging_process_management_service.service.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InternalPasswordEncoder {

  private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  public InternalPasswordEncoder() {
    this.passwordEncoder = new BCryptPasswordEncoder(); // Instantiate the BCryptPasswordEncoder
  }

  public String encodePassword(String rawPassword) {
    // Hash the plain text password using BCrypt
    return passwordEncoder.encode(rawPassword);
  }

}
