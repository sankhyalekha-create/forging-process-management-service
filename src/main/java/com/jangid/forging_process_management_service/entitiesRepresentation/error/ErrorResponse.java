package com.jangid.forging_process_management_service.entitiesRepresentation.error;

public class ErrorResponse {
  private String message;

  public ErrorResponse(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}

