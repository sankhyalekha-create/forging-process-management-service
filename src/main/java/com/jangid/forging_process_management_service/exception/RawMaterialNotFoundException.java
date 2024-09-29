package com.jangid.forging_process_management_service.exception;

public class RawMaterialNotFoundException extends RuntimeException {

  private static final String MESSAGE_FORMAT = "RawMaterial not found. imsOrgId=%s message=%s";

  public RawMaterialNotFoundException(String message) {
    super(message);
  }

  public RawMaterialNotFoundException(String imsOrgId, String message) {
    super(String.format(MESSAGE_FORMAT, imsOrgId, message));
  }
}
