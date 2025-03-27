package com.jangid.forging_process_management_service.entities.security;

public class TenantContext {

  private static final ThreadLocal<String> tenantHolder = new ThreadLocal<>();

  public static void setCurrentTenant(String tenantName) {
    tenantHolder.set(tenantName);
  }

  public static String getCurrentTenant() {
    return tenantHolder.get();
  }

  public static void clear() {
    tenantHolder.remove();
  }
}
