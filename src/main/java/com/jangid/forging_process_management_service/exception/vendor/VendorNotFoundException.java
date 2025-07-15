package com.jangid.forging_process_management_service.exception.vendor;

public class VendorNotFoundException extends RuntimeException {
    public VendorNotFoundException(String message) {
        super(message);
    }

    public VendorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public VendorNotFoundException(Long vendorId) {
        super("Vendor not found with id: " + vendorId);
    }

    public VendorNotFoundException(Long tenantId, Long vendorId) {
        super("Vendor not found with id: " + vendorId + " for tenant: " + tenantId);
    }
} 