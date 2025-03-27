package com.jangid.forging_process_management_service.service.security;

import com.jangid.forging_process_management_service.repositories.security.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String usernameWithTenant) throws UsernameNotFoundException {
    String[] parts = usernameWithTenant.split("@");
    if (parts.length != 2) {
      throw new UsernameNotFoundException("Invalid username format");
    }
    String username = parts[0];
    String tenantName = parts[1];

    return userRepository.findByUsernameAndTenant_TenantNameAndDeletedFalse(username, tenantName)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
