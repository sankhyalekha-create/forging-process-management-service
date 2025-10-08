package com.jangid.forging_process_management_service.resource.security;

import com.jangid.forging_process_management_service.entitiesRepresentation.security.PasswordUpdateRepresentation;
import com.jangid.forging_process_management_service.service.security.PasswordUpdateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PasswordUpdateResource {

  private final PasswordUpdateService passwordUpdateService;

  @PutMapping("tenant/{tenantId}/user/updatePassword")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<Map<String, String>> updatePassword(
      @PathVariable Long tenantId,
      @RequestBody PasswordUpdateRepresentation passwordUpdateRequest) {
    
    try {
      // Validate input
      if (isInvalidPasswordUpdateRequest(passwordUpdateRequest)) {
        log.error("Invalid password update request");
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Invalid input. Please provide username, current password, and new password."));
      }

      // Set tenant ID from path variable
      passwordUpdateRequest.setTenantId(tenantId);

      // Attempt to update password
      boolean updateSuccess = passwordUpdateService.updatePassword(passwordUpdateRequest);

      if (updateSuccess) {
        log.info("Password updated successfully for user: {}", passwordUpdateRequest.getUsername());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
      } else {
        log.error("Password update failed for user: {}", passwordUpdateRequest.getUsername());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Password update failed. Please check your current password and try again."));
      }

    } catch (Exception exception) {
      log.error("Error updating password", exception);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal server error occurred while updating password"));
    }
  }

  private boolean isInvalidPasswordUpdateRequest(PasswordUpdateRepresentation request) {
    return request == null ||
           request.getUsername() == null || request.getUsername().trim().isEmpty() ||
           request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty() ||
           request.getNewPassword() == null || request.getNewPassword().trim().isEmpty();
  }
} 