package com.jangid.forging_process_management_service.entitiesRepresentation.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigurationResponse {
  
  private Long tenantId;
  private String tenantName;
  private Map<String, Object> configurations;
  private LocalDateTime lastUpdated;
}
