package com.jangid.forging_process_management_service.assemblers.buyer;

import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerEntityRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class BuyerEntityAssembler {

    public BuyerEntity createAssemble(BuyerEntityRepresentation representation) {
        BuyerEntity buyerEntity = assemble(representation);
        buyerEntity.setCreatedAt(LocalDateTime.now());
        return buyerEntity;
    }

    public BuyerEntity assemble(BuyerEntityRepresentation representation) {
        return BuyerEntity.builder()
            .id(representation.getId())
            .buyerEntityName(representation.getBuyerEntityName())
            .address(representation.getAddress())
            .gstinUin(representation.getGstinUin())
            .phoneNumber(representation.getPhoneNumber())
            .email(representation.getEmail())
            .panNumber(representation.getPanNumber())
            .stateCode(representation.getStateCode())
            .pincode(representation.getPincode())
            .isBillingEntity(representation.isBillingEntity())
            .isShippingEntity(representation.isShippingEntity())
            .build();
    }

    public BuyerEntityRepresentation dissemble(BuyerEntity buyerEntity) {
        return BuyerEntityRepresentation.builder()
            .id(buyerEntity.getId())
            .buyerEntityName(buyerEntity.getBuyerEntityName())
            .address(buyerEntity.getAddress())
            .gstinUin(buyerEntity.getGstinUin())
            .phoneNumber(buyerEntity.getPhoneNumber())
            .email(buyerEntity.getEmail())
            .panNumber(buyerEntity.getPanNumber())
            .stateCode(buyerEntity.getStateCode())
            .pincode(buyerEntity.getPincode())
            .isBillingEntity(buyerEntity.isBillingEntity())
            .isShippingEntity(buyerEntity.isShippingEntity())
            .buyerId(buyerEntity.getBuyer().getId())
            .build();
    }
} 