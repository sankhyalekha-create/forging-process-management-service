package com.jangid.forging_process_management_service.assemblers.buyer;

import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerEntityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BuyerAssembler {

  private final BuyerEntityAssembler buyerEntityAssembler;

  @Autowired
  public BuyerAssembler(BuyerEntityAssembler buyerEntityAssembler) {
    this.buyerEntityAssembler = buyerEntityAssembler;
  }

  public Buyer createAssemble(BuyerRepresentation representation) {
    Buyer buyer = assemble(representation);
    buyer.setCreatedAt(LocalDateTime.now());
    return buyer;
  }

  public Buyer assemble(BuyerRepresentation representation) {

    return Buyer.builder()
        .id(representation.getId())
        .buyerName(representation.getBuyerName())
        .address(representation.getAddress())
        .gstinUin(representation.getGstinUin())
        .phoneNumber(representation.getPhoneNumber())
        .entities(representation.getEntities() != null ? representation.getEntities().stream()
            .map(buyerEntityAssembler::assemble)
            .collect(Collectors.toList()) : new ArrayList<>())
        .build();
  }

  public BuyerRepresentation dissemble(Buyer buyer) {
    List<BuyerEntityRepresentation> entityRepresentations = buyer.getEntities().stream()
        .map(buyerEntityAssembler::dissemble)
        .collect(Collectors.toList());

    return BuyerRepresentation.builder()
        .id(buyer.getId())
        .buyerName(buyer.getBuyerName())
        .address(buyer.getAddress())
        .gstinUin(buyer.getGstinUin())
        .phoneNumber(buyer.getPhoneNumber())
        .entities(entityRepresentations)
        .build();
  }
} 