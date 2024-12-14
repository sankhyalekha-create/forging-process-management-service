package com.jangid.forging_process_management_service.entitiesRepresentation.heating;

public class HeatTreatmentBatchNotInExpectedStatusException extends RuntimeException{

  public HeatTreatmentBatchNotInExpectedStatusException(String message) {
    super(message);
  }

}
