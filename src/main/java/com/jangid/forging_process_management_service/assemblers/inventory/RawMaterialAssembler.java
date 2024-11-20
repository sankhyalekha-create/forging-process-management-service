package com.jangid.forging_process_management_service.assemblers.inventory;

import com.jangid.forging_process_management_service.assemblers.product.ProductAssembler;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.service.product.ProductService;
import com.jangid.forging_process_management_service.utils.ConstantUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RawMaterialAssembler {

  @Autowired
  private ProductService productService;

  public RawMaterialRepresentation dissemble(RawMaterial rawMaterial) {

    List<RawMaterialProductRepresentation> rawMaterialProductRepresentations = new ArrayList<>();
    rawMaterial.getRawMaterialProducts().forEach(rmp -> rawMaterialProductRepresentations.add(RawMaterialProductRepresentation.builder()
                                                                                                  .id(rmp.getId())
                                                                                                  .rawMaterialId(String.valueOf(rmp.getRawMaterial().getId()))
                                                                                                  .product(ProductAssembler.dissemble(rmp.getProduct()))
                                                                                                  .heats(RawMaterialHeatAssembler.getHeatRepresentations(rmp.getHeats()))
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
        .supplier(SupplierRepresentation.builder().id(rawMaterial.getSupplier().getId()).supplierName(rawMaterial.getSupplier().getSupplierName()).supplierDetail(rawMaterial.getSupplier().getSupplierDetail()).build())
        .rawMaterialProducts(rawMaterialProductRepresentations)
        .createdAt(rawMaterial.getCreatedAt() != null ? rawMaterial.getCreatedAt().toString() : null)
        .build();
  }

  public RawMaterial createAssemble(RawMaterialRepresentation rawMaterialRepresentation) {
    List<RawMaterialProduct> rawMaterialProducts = new ArrayList<>();
    rawMaterialRepresentation.getRawMaterialProducts().forEach(rmp -> {
      RawMaterialProduct rawMaterialProduct = RawMaterialProductAssembler.createAssemble(rmp);
      rawMaterialProduct.setCreatedAt(LocalDateTime.now());
      if (rmp.getProduct() != null) {
        rawMaterialProduct.setProduct(productService.getProductById(rmp.getProduct().getId()));
      }
      rawMaterialProducts.add(rawMaterialProduct);
    });
    RawMaterial rawMaterial =  RawMaterial.builder()
        .rawMaterialInvoiceDate(
            rawMaterialRepresentation.getRawMaterialInvoiceDate() != null ? LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialInvoiceDate(), ConstantUtils.DATE_TIME_FORMATTER) : null)
        .poNumber(rawMaterialRepresentation.getPoNumber())
        .rawMaterialReceivingDate(
            rawMaterialRepresentation.getRawMaterialReceivingDate() != null ? LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialReceivingDate(), ConstantUtils.DATE_TIME_FORMATTER) : null)
        .rawMaterialInvoiceNumber(rawMaterialRepresentation.getRawMaterialInvoiceNumber())
        .rawMaterialTotalQuantity(rawMaterialRepresentation.getRawMaterialTotalQuantity() != null ? Double.valueOf(rawMaterialRepresentation.getRawMaterialTotalQuantity()) : null)
        .rawMaterialHsnCode(rawMaterialRepresentation.getRawMaterialHsnCode())
        .rawMaterialGoodsDescription(rawMaterialRepresentation.getRawMaterialGoodsDescription())
        .createdAt(rawMaterialRepresentation.getCreatedAt() != null ? LocalDateTime.parse(rawMaterialRepresentation.getCreatedAt(), ConstantUtils.DATE_TIME_FORMATTER) : null)
        .build();
    rawMaterial.updateRawMaterialProducts(rawMaterialProducts);
    if(rawMaterial.getRawMaterialProducts()!=null){
      rawMaterial.getRawMaterialProducts().clear();
    }
    rawMaterial.setRawMaterialProducts(rawMaterialProducts);
    return rawMaterial;
  }

}
