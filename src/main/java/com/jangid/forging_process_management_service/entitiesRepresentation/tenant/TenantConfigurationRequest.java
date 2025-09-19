package com.jangid.forging_process_management_service.entitiesRepresentation.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigurationRequest {
  private Map<String, Object> configurations;
}
