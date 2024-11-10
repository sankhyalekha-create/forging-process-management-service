package com.jangid.forging_process_management_service.exception.product;

public class SupplierNotFoundException  extends RuntimeException{
  private static final String MESSAGE_FORMAT = "Supplier not found. supplierId=%s tenantId=%s";

  public SupplierNotFoundException(String message){
    super(message);
  }
  public SupplierNotFoundException(String supplierId, String tenantId) {
    super(String.format(MESSAGE_FORMAT, supplierId, tenantId));
  }

}
