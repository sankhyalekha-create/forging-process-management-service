package com.jangid.forging_process_management_service.exception.transporter;

/**
 * Exception thrown when a requested transporter is not found in the system.
 */
public class TransporterNotFoundException extends RuntimeException {
  
  /**
   * Constructs a new TransporterNotFoundException with the specified detail message.
   *
   * @param message the detail message
   */
  public TransporterNotFoundException(String message) {
    super(message);
  }
  
  /**
   * Constructs a new TransporterNotFoundException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public TransporterNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}

