package com.jangid.forging_process_management_service.assemblers.inventory;

import com.jangid.forging_process_management_service.assemblers.product.ProductAssembler;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RawMaterialProductAssembler {

  public RawMaterialProductRepresentation dissemble(RawMaterialProduct rawMaterialProduct) {
    return RawMaterialProductRepresentation.builder()
        .id(rawMaterialProduct.getId())
        .rawMaterial(rawMaterialProduct.getRawMaterial() != null ? dissemble(rawMaterialProduct.getRawMaterial()) : null)
        .product(ProductAssembler.dissemble(rawMaterialProduct.getProduct()))
        .heats(getHeatRepresentations(rawMaterialProduct.getHeats()))
        .createdAt(rawMaterialProduct.getCreatedAt() != null ? rawMaterialProduct.getCreatedAt().toString() : null).build();
  }

  public RawMaterialProduct createAssemble(RawMaterialProductRepresentation rawMaterialProductRepresentation) {
    List<Heat> heats = getCreateHeats(rawMaterialProductRepresentation.getHeats());
    RawMaterialProduct rawMaterialProduct = RawMaterialProduct.builder()
        .id(rawMaterialProductRepresentation.getId())
        .build();
    heats.forEach(rawMaterialProduct::setRawMaterialProduct);
    if (rawMaterialProduct.getHeats() != null) {
      rawMaterialProduct.getHeats().clear();
    }
    rawMaterialProduct.setHeats(heats);
    return rawMaterialProduct;
  }

  public RawMaterialProduct assemble(RawMaterialProductRepresentation rawMaterialProductRepresentation) {
    List<Heat> heats = getHeats(rawMaterialProductRepresentation.getHeats());
    RawMaterialProduct rawMaterialProduct = RawMaterialProduct.builder()
        .id(rawMaterialProductRepresentation.getId())
        .build();
    heats.forEach(rawMaterialProduct::setRawMaterialProduct);
    if (rawMaterialProduct.getHeats() != null) {
      rawMaterialProduct.getHeats().clear();
    }
    rawMaterialProduct.setHeats(heats);
    return rawMaterialProduct;
  }

  public List<RawMaterialProduct> getRawMaterialProducts(List<RawMaterialProductRepresentation> representationList) {
    return representationList.stream()
        .map(this::assemble)
        .collect(Collectors.toList());
  }

  public List<HeatRepresentation> getHeatRepresentations(List<Heat> heats) {
    List<HeatRepresentation> heatRepresentations = new ArrayList<>();
    heats.forEach(heat -> {
      heatRepresentations.add(dissemble(heat));
    });
    return heatRepresentations;
  }

  public HeatRepresentation dissemble(Heat heat) {
    return HeatRepresentation.builder()
        .id(heat.getId())
        .heatNumber(heat.getHeatNumber())
        .heatQuantity(String.valueOf(heat.getHeatQuantity()))
        .availableHeatQuantity(String.valueOf(heat.getAvailableHeatQuantity()))
        .testCertificateNumber(heat.getTestCertificateNumber())
        .location(heat.getLocation())
        .createdAt(heat.getCreatedAt() != null ? heat.getCreatedAt().toString() : null)
        .build();
  }

  public List<Heat> getCreateHeats(List<HeatRepresentation> heatRepresentations) {
    List<Heat> heats = new ArrayList<>();
    heatRepresentations.forEach(heatRepresentation -> {
      Heat heat = assemble(heatRepresentation);
      heat.setCreatedAt(LocalDateTime.now());
      heats.add(heat);
    });
    return heats;
  }

  public Heat assemble(HeatRepresentation heatRepresentation) {
    return Heat.builder()
        .heatNumber(heatRepresentation.getHeatNumber())
        .isInPieces(heatRepresentation.getIsInPieces())
        .heatQuantity(heatRepresentation.getHeatQuantity() != null ? Double.valueOf(heatRepresentation.getHeatQuantity()) : null)
        .piecesCount(heatRepresentation.getPiecesCount() != null ? heatRepresentation.getPiecesCount() : null)
        .availablePiecesCount(heatRepresentation.getAvailablePiecesCount() != null ? heatRepresentation.getAvailablePiecesCount() : null)
        .testCertificateNumber(heatRepresentation.getTestCertificateNumber())
        .availableHeatQuantity(heatRepresentation.getHeatQuantity() != null ? Double.valueOf(heatRepresentation.getHeatQuantity()) : null)
        .location(heatRepresentation.getLocation())
        .build();
  }

  public List<Heat> getHeats(List<HeatRepresentation> heatRepresentations) {
    List<Heat> heats = new ArrayList<>();
    heatRepresentations.forEach(heatRepresentation -> {
      Heat heat = assemble(heatRepresentation);
      heats.add(heat);
    });
    return heats;
  }

  public RawMaterialRepresentation dissemble(RawMaterial rawMaterial) {

    List<RawMaterialProductRepresentation> rawMaterialProductRepresentations = new ArrayList<>();
    rawMaterial.getRawMaterialProducts().forEach(rmp -> rawMaterialProductRepresentations.add(RawMaterialProductRepresentation.builder()
                                                                                                  .id(rmp.getId())
                                                                                                  .product(ProductAssembler.dissemble(rmp.getProduct()))
//                                                                                                  .heats(rawMaterialHeatAssembler.getHeatRepresentations(rmp.getHeats()))
                                                                                                  .build()));
    return RawMaterialRepresentation.builder()
        .id(rawMaterial.getId())
        .tenantId(rawMaterial.getTenant().getId())
        .rawMaterialInvoiceDate(rawMaterial.getRawMaterialInvoiceDate() != null ? rawMaterial.getRawMaterialInvoiceDate().toString() : null)
        .poNumber(rawMaterial.getPoNumber())
        .rawMaterialReceivingDate(rawMaterial.getRawMaterialReceivingDate() != null ? rawMaterial.getRawMaterialReceivingDate().toString() : null)
        .rawMaterialInvoiceNumber(rawMaterial.getRawMaterialInvoiceNumber())
        .rawMaterialTotalQuantity(String.valueOf(rawMaterial.getRawMaterialTotalQuantity()))
        .rawMaterialHsnCode(rawMaterial.getRawMaterialHsnCode())
        .rawMaterialGoodsDescription(rawMaterial.getRawMaterialGoodsDescription())
        .supplier(SupplierRepresentation.builder().id(rawMaterial.getSupplier().getId()).supplierName(rawMaterial.getSupplier().getSupplierName())
                      .supplierDetail(rawMaterial.getSupplier().getSupplierDetail()).build())
        .rawMaterialProducts(rawMaterialProductRepresentations)
        .createdAt(rawMaterial.getCreatedAt() != null ? rawMaterial.getCreatedAt().toString() : null)
        .build();
  }

}
