package com.jangid.forging_process_management_service.service.security;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.security.Usr;
import com.jangid.forging_process_management_service.entitiesRepresentation.security.UserRegistrationRepresentation;
import com.jangid.forging_process_management_service.repositories.security.UserRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserRegistrationService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private InternalPasswordEncoder internalPasswordEncoder;

  @Transactional
  public UserRegistrationRepresentation registerUser(long tenantId, UserRegistrationRepresentation representation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    String encodedPasswd = internalPasswordEncoder.encodePassword(representation.getPassword());
    Usr usr = Usr.builder()
        .username(representation.getUsername())
        .password(encodedPasswd)
        .tenant(tenant)
        .roles(representation.getRoles())
        .createdAt(LocalDateTime.now())
        .build();
    Usr savedUser = userRepository.save(usr);

    return UserRegistrationRepresentation.builder()
        .username(savedUser.getUsername())
        .roles(savedUser.getRoles())
        .build();
  }

}
