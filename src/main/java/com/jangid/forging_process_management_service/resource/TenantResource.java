package com.jangid.forging_process_management_service.resource;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class TenantResource {

  @Autowired
  private final TenantService tenantService;


  @GetMapping(value = "/tenant/{id}")
  public ResponseEntity<Tenant> getTenantById(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("id") String id) {
    Long tenantId = GenericResourceUtils.convertResourceIdToLong(id)
        .orElseThrow(() -> new RuntimeException("Not valid id!"));

    Tenant tenant = tenantService.getTenantById(tenantId);
    return ResponseEntity.ok(tenant);
  }

  @GetMapping("/tenants")
  public ResponseEntity<List<Tenant>> getTenants() {
    List<Tenant> tenants = tenantService.getAllTenants();
    return ResponseEntity.ok(tenants);
  }

  @PostMapping("/tenants")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<Tenant> createTenant(@Validated @RequestBody Tenant tenant) {
    try {
      if (tenant.getTenantName() == null || tenant.getTenantOrgId() == null) {
        log.error("invalid input of Tenant!");
        throw new RuntimeException("invalid input of Tenant!");
      }
      Tenant createdTenant = tenantService.createTenant(tenant);
      return new ResponseEntity<>(createdTenant, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }

}
