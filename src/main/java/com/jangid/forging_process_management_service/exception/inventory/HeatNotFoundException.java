package com.jangid.forging_process_management_service.exception.inventory;

public class HeatNotFoundException extends RuntimeException {

  private static final String MESSAGE_FORMAT = "Heat not found.";

  public HeatNotFoundException(String message) {
    super(message);
  }
}
