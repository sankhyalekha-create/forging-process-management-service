package com.jangid.forging_process_management_service.exception.machining;

public class MachineNotFoundException extends RuntimeException{

  public MachineNotFoundException(String message) {
    super(message);
  }
}
