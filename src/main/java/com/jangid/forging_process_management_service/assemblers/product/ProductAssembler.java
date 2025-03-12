package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.Supplier;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;

import java.util.List;
import java.util.stream.Collectors;

public class ProductAssembler {

  public static ProductRepresentation dissemble(Product product) {
    return ProductRepresentation.builder()
        .id(product.getId())
        .productName(product.getProductName())
        .productCode(product.getProductCode())
        .unitOfMeasurement(product.getUnitOfMeasurement().name())
        .suppliers(getSupplierRepresentations(product.getSuppliers()))
        .tenantId(product.getTenant().getId())
        .build();
  }

  public static Product assemble(ProductRepresentation productRepresentation) {
    return Product.builder()
        .id(productRepresentation.getId())
        .productName(productRepresentation.getProductName())
        .productCode(productRepresentation.getProductCode())
        .unitOfMeasurement(UnitOfMeasurement.valueOf(productRepresentation.getUnitOfMeasurement()))
        .build();
  }

  private static List<Supplier> getSuppliers(List<SupplierRepresentation> supplierRepresentations) {
    return supplierRepresentations.stream()
        .map(SupplierAssembler::assemble)
        .collect(Collectors.toList());
  }

  private static List<SupplierRepresentation> getSupplierRepresentations(List<Supplier> suppliers) {
    return suppliers.stream()
        .map(SupplierAssembler::dissemble)
        .collect(Collectors.toList());
  }
}
