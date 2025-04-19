package com.jangid.forging_process_management_service.resource.security;

import com.jangid.forging_process_management_service.entitiesRepresentation.security.UserRegistrationRepresentation;
import com.jangid.forging_process_management_service.service.security.UserRegistrationService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class UserRegistrationResource {

  @Autowired
  private final UserRegistrationService userRegistrationService;

  @PostMapping("tenant/1/registerUser")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<UserRegistrationRepresentation> registerUser(@RequestBody UserRegistrationRepresentation userRegistrationRepresentation) {
    try {
      if (userRegistrationRepresentation.getTenantId() == null ||  isInValidUserRegistrationRepresentation(userRegistrationRepresentation)) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
//      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
//          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      UserRegistrationRepresentation registeredUser = userRegistrationService.registerUser(userRegistrationRepresentation.getTenantId(), userRegistrationRepresentation);
      return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }

  private boolean isInValidUserRegistrationRepresentation(UserRegistrationRepresentation representation){
    if (representation == null ||
        representation.getUsername() == null || representation.getUsername().isEmpty() ||
        representation.getPassword() == null || representation.getPassword().isEmpty() ||
        representation.getRoles() == null || representation.getRoles().isEmpty()
    ) {
      log.error("Invalid UserRegistrationRepresentation input!");
      return true;
    }
    return false;
  }


}
