package com.jangid.forging_process_management_service.exception.forging;

public class ForgeNotInExpectedStatusException extends RuntimeException{

  public ForgeNotInExpectedStatusException(String message) {
    super(message);
  }

}
