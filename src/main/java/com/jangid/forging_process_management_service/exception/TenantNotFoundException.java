package com.jangid.forging_process_management_service.exception;

public class TenantNotFoundException extends RuntimeException {

  private static final String MESSAGE_FORMAT = "Tenant not found. imsOrgId=%s message=%s";

  public TenantNotFoundException(String tenantId) {
    super("Tenant not found. tenantId=" + tenantId);
  }

  public TenantNotFoundException(String imsOrgId, String message) {
    super(String.format(MESSAGE_FORMAT, imsOrgId, message));
  }
}
