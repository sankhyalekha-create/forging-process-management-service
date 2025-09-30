package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.entities.product.Supplier;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;

public class SupplierAssembler {

  public static SupplierRepresentation dissemble(Supplier supplier){
    return SupplierRepresentation.builder()
        .id(supplier.getId())
        .supplierName(supplier.getSupplierName())
        .supplierDetail(supplier.getSupplierDetail())
        .address(supplier.getAddress())
        .phoneNumber(supplier.getPhoneNumber())
        .email(supplier.getEmail())
        .panNumber(supplier.getPanNumber())
        .gstinNumber(supplier.getGstinNumber())
        .stateCode(supplier.getStateCode())
        .pincode(supplier.getPincode())
        .tenantId(supplier.getTenant().getId())
        .createdAt(supplier.getCreatedAt()!=null?supplier.getCreatedAt().toString():null).build();
  }

  public static Supplier assemble(SupplierRepresentation supplierRepresentation){
    return Supplier.builder()
        .id(supplierRepresentation.getId())
        .supplierName(supplierRepresentation.getSupplierName())
        .supplierDetail(supplierRepresentation.getSupplierDetail())
        .address(supplierRepresentation.getAddress())
        .phoneNumber(supplierRepresentation.getPhoneNumber())
        .email(supplierRepresentation.getEmail())
        .panNumber(supplierRepresentation.getPanNumber())
        .gstinNumber(supplierRepresentation.getGstinNumber())
        .stateCode(supplierRepresentation.getStateCode())
        .pincode(supplierRepresentation.getPincode())
        .build();
  }

}
