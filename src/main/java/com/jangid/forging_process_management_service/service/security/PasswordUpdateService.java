package com.jangid.forging_process_management_service.service.security;

import com.jangid.forging_process_management_service.entities.security.Usr;
import com.jangid.forging_process_management_service.entitiesRepresentation.security.PasswordUpdateRepresentation;
import com.jangid.forging_process_management_service.repositories.security.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordUpdateService {

  private final UserRepository userRepository;
  private final InternalPasswordEncoder internalPasswordEncoder;

  @Transactional
  public boolean updatePassword(PasswordUpdateRepresentation passwordUpdateRequest) {
    try {
      // Find user by username and tenant
      Optional<Usr> userOptional;
      if (passwordUpdateRequest.getTenantId() != null) {
        userOptional = userRepository.findByUsernameAndTenant_IdAndDeletedFalse(
            passwordUpdateRequest.getUsername(), 
            passwordUpdateRequest.getTenantId()
        );
      } else if (passwordUpdateRequest.getTenantName() != null) {
        userOptional = userRepository.findByUsernameAndTenant_TenantNameAndDeletedFalse(
            passwordUpdateRequest.getUsername(), 
            passwordUpdateRequest.getTenantName()
        );
      } else {
        log.error("Either tenantId or tenantName must be provided");
        return false;
      }

      if (userOptional.isEmpty()) {
        log.error("User not found: {} for tenant", passwordUpdateRequest.getUsername());
        return false;
      }

      Usr user = userOptional.get();

      // Verify current password
      if (!verifyCurrentPassword(passwordUpdateRequest.getCurrentPassword(), user.getPassword())) {
        log.error("Current password verification failed for user: {}", passwordUpdateRequest.getUsername());
        return false;
      }

      // Validate new password
      if (!isValidNewPassword(passwordUpdateRequest.getNewPassword())) {
        log.error("New password validation failed for user: {}", passwordUpdateRequest.getUsername());
        return false;
      }

      // Encode new password
      String encodedNewPassword = internalPasswordEncoder.encodePassword(passwordUpdateRequest.getNewPassword());

      // Update password
      user.setPassword(encodedNewPassword);
      user.setUpdatedAt(LocalDateTime.now());

      // Save updated user
      userRepository.save(user);

      log.info("Password updated successfully for user: {}", passwordUpdateRequest.getUsername());
      return true;

    } catch (Exception e) {
      log.error("Error updating password for user: {}", passwordUpdateRequest.getUsername(), e);
      return false;
    }
  }

  private boolean verifyCurrentPassword(String currentPassword, String storedPassword) {
    // Use BCrypt to verify the current password
    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
        .matches(currentPassword, storedPassword);
  }

  private boolean isValidNewPassword(String newPassword) {
    if (newPassword == null || newPassword.trim().isEmpty()) {
      return false;
    }
    
    // Add password strength validation if needed
    // For now, just check minimum length
    return newPassword.length() >= 6;
  }
} 