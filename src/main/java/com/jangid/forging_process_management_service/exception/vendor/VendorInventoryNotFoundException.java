package com.jangid.forging_process_management_service.exception.vendor;

import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;

/**
 * Exception thrown when a vendor inventory is not found.
 */
public class VendorInventoryNotFoundException extends ResourceNotFoundException {

    public VendorInventoryNotFoundException(String message) {
        super(message);
    }

    public VendorInventoryNotFoundException(Long vendorInventoryId) {
        super("Vendor inventory not found with id: " + vendorInventoryId);
    }
} 