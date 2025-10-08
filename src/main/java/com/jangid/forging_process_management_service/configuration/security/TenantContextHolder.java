package com.jangid.forging_process_management_service.configuration.security;

import lombok.Builder;
import lombok.Data;

/**
 * Holder for authenticated tenant context extracted from JWT token.
 * This ensures tenant information is derived from authentication, not URL parameters.
 */
@Data
@Builder
public class TenantContextHolder {
  private Long tenantId;
  private String tenantName;
  private String username;

  private static final ThreadLocal<TenantContextHolder> contextHolder = new ThreadLocal<>();

  public static void setContext(TenantContextHolder context) {
    contextHolder.set(context);
  }

  public static TenantContextHolder getContext() {
    return contextHolder.get();
  }

  public static void clear() {
    contextHolder.remove();
  }

  /**
   * Get the authenticated tenant ID from the current security context.
   * This should be used instead of accepting tenant ID from URL parameters.
   */
  public static Long getAuthenticatedTenantId() {
    TenantContextHolder context = getContext();
    if (context == null) {
      throw new SecurityException("No authenticated tenant context found. User must be authenticated.");
    }
    return context.getTenantId();
  }

  /**
   * Get the authenticated tenant name from the current security context.
   */
  public static String getAuthenticatedTenantName() {
    TenantContextHolder context = getContext();
    if (context == null) {
      throw new SecurityException("No authenticated tenant context found. User must be authenticated.");
    }
    return context.getTenantName();
  }

  /**
   * Validates that the provided tenant ID matches the authenticated tenant.
   * Throws SecurityException if there's a mismatch.
   * 
   * @param requestedTenantId The tenant ID from the request (URL or body)
   * @throws SecurityException if tenant IDs don't match
   */
  public static void validateTenantAccess(Long requestedTenantId) {
    Long authenticatedTenantId = getAuthenticatedTenantId();
    if (!authenticatedTenantId.equals(requestedTenantId)) {
      throw new SecurityException(
        String.format("Access denied. User is authenticated for tenant %d but requested access to tenant %d",
          authenticatedTenantId, requestedTenantId)
      );
    }
  }
}

