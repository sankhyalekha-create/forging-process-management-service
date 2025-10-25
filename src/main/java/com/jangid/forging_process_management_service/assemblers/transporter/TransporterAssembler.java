package com.jangid.forging_process_management_service.assemblers.transporter;

import com.jangid.forging_process_management_service.entities.transporter.Transporter;
import com.jangid.forging_process_management_service.entitiesRepresentation.transporter.TransporterRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Assembler for converting between Transporter entity and TransporterRepresentation.
 * Handles bidirectional conversion for API serialization/deserialization.
 */
@Slf4j
@Component
public class TransporterAssembler {
  
  /**
   * Creates a new Transporter entity from representation with creation timestamp.
   *
   * @param representation the transporter representation
   * @return newly assembled Transporter entity with creation timestamp
   */
  public Transporter createAssemble(TransporterRepresentation representation) {
    Transporter transporter = assemble(representation);
    transporter.setCreatedAt(LocalDateTime.now());
    return transporter;
  }
  
  /**
   * Assembles a Transporter entity from representation.
   *
   * @param representation the transporter representation
   * @return assembled Transporter entity
   */
  public Transporter assemble(TransporterRepresentation representation) {
    return Transporter.builder()
      .id(representation.getId())
      .transporterName(representation.getTransporterName())
      .gstin(representation.getGstin())
      .transporterIdNumber(representation.getTransporterIdNumber())
      .panNumber(representation.getPanNumber())
      .address(representation.getAddress())
      .stateCode(representation.getStateCode())
      .pincode(representation.getPincode())
      .phoneNumber(representation.getPhoneNumber())
      .alternatePhoneNumber(representation.getAlternatePhoneNumber())
      .email(representation.getEmail())
      .isGstRegistered(representation.isGstRegistered())
      .bankAccountNumber(representation.getBankAccountNumber())
      .ifscCode(representation.getIfscCode())
      .bankName(representation.getBankName())
      .notes(representation.getNotes())
      .build();
  }
  
  /**
   * Disassembles a Transporter entity into representation.
   *
   * @param transporter the transporter entity
   * @return disassembled TransporterRepresentation
   */
  public TransporterRepresentation dissemble(Transporter transporter) {
    return TransporterRepresentation.builder()
      .id(transporter.getId())
      .transporterName(transporter.getTransporterName())
      .gstin(transporter.getGstin())
      .transporterIdNumber(transporter.getTransporterIdNumber())
      .panNumber(transporter.getPanNumber())
      .address(transporter.getAddress())
      .stateCode(transporter.getStateCode())
      .pincode(transporter.getPincode())
      .phoneNumber(transporter.getPhoneNumber())
      .alternatePhoneNumber(transporter.getAlternatePhoneNumber())
      .email(transporter.getEmail())
      .isGstRegistered(transporter.isGstRegistered())
      .bankAccountNumber(transporter.getBankAccountNumber())
      .ifscCode(transporter.getIfscCode())
      .bankName(transporter.getBankName())
      .notes(transporter.getNotes())
      .build();
  }
}

