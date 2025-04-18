package com.jangid.forging_process_management_service.exception.buyer;

public class BuyerNotFoundException extends RuntimeException{
  private static final String MESSAGE_FORMAT = "Buyer not found. buyerId=%s tenantId=%s";

  public BuyerNotFoundException(String message){
    super(message);
  }
  public BuyerNotFoundException(String buyerId, String tenantId) {
    super(String.format(MESSAGE_FORMAT, buyerId, tenantId));
  }

}
