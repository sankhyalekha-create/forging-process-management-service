package com.jangid.forging_process_management_service.assemblers.operator;

import com.jangid.forging_process_management_service.entities.operator.Operator;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorRepresentation;
import com.jangid.forging_process_management_service.service.operator.OperatorService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;


@Slf4j
@Component
public class OperatorAssembler {

  private final OperatorService operatorService;

  @Autowired
  public OperatorAssembler(@Lazy OperatorService operatorService) {
    this.operatorService = operatorService;
  }

  public Operator createAssemble(OperatorRepresentation representation) {
    Operator operator = assemble(representation);

    operator.setCreatedAt(LocalDateTime.now());
    return operator;
  }

  public Operator assemble(OperatorRepresentation representation) {
    if(representation.getId()!=null){
      return operatorService.getOperatorById(representation.getId());
    }
    return Operator.builder()
        .id(representation.getId())
        .fullName(representation.getFullName())
        .address(representation.getAddress())
        .aadhaarNumber(representation.getAadhaarNumber())
        .phoneNumber(representation.getPhoneNumber())
        .previousTenantIds(representation.getPreviousTenantIds() != null ? new ArrayList<>(representation.getPreviousTenantIds()) : new ArrayList<>())
        .build();
  }

  public OperatorRepresentation dissemble(Operator operator) {
    return OperatorRepresentation.builder()
        .id(operator.getId())
        .fullName(operator.getFullName())
        .address(operator.getAddress())
        .aadhaarNumber(operator.getAadhaarNumber())
        .phoneNumber(operator.getPhoneNumber())
        .tenantId(operator.getTenant().getId())
        .previousTenantIds(operator.getPreviousTenantIds() != null ? new ArrayList<>(operator.getPreviousTenantIds()) : new ArrayList<>())
        .build();
  }
}

