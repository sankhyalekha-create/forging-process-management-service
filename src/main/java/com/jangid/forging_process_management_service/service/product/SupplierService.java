package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.SupplierAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.Supplier;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.product.SupplierNotFoundException;
import com.jangid.forging_process_management_service.repositories.product.ProductRepository;
import com.jangid.forging_process_management_service.repositories.product.SupplierRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SupplierService {

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private TenantService tenantService;

  @Transactional
  public SupplierRepresentation createSupplier(long tenantId, SupplierRepresentation supplierRepresentation){
    Tenant tenant = tenantService.getTenantById(tenantId);
    Supplier supplier = SupplierAssembler.assemble(supplierRepresentation);
    supplier.setTenant(tenant);
    supplier.setCreatedAt(LocalDateTime.now());
    Supplier createdSupplier = supplierRepository.save(supplier);
    return SupplierAssembler.dissemble(createdSupplier);
  }

  public Page<SupplierRepresentation> getAllSuppliersOfTenant(long tenantId, int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    Page<Supplier> supplierPage = supplierRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);
    return supplierPage.map(SupplierAssembler::dissemble);
  }

  public SupplierListRepresentation getAllSuppliersOfTenantWithoutPagination(long tenantId){
    List<Supplier> suppliers = supplierRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
    return SupplierListRepresentation.builder().supplierRepresentations(suppliers.stream().map(SupplierAssembler::dissemble).toList()).build();
  }



  public SupplierRepresentation getSupplierOfTenant(long tenantId, long supplierId){
    Supplier supplier = getSupplierById(supplierId);
    if(supplier.getTenant().getId()!=tenantId){
      throw new SupplierNotFoundException("Supplier not found with supplierId="+supplierId);
    }
    return SupplierAssembler.dissemble(supplier);
  }

  @Transactional
  public SupplierRepresentation updateSupplier(long tenantId, long supplierId, SupplierRepresentation representation){
    Tenant tenant = tenantService.getTenantById(tenantId);
    Supplier existingSupplier = getSupplierByIdAndTenantId(supplierId, tenant.getId());

    if (!existingSupplier.getSupplierName().equals(representation.getSupplierName())) {
      existingSupplier.setSupplierName(representation.getSupplierName());
    }
    if (!existingSupplier.getSupplierDetail().equals(representation.getSupplierDetail())) {
      existingSupplier.setSupplierDetail(representation.getSupplierDetail());
    }
    Supplier updatedSupplier = supplierRepository.save(existingSupplier);
    return SupplierAssembler.dissemble(updatedSupplier);
  }

  public Supplier getSupplierByIdAndTenantId(long supplierId, long tenantId){
    Optional<Supplier> optionalSupplier = supplierRepository.findByIdAndTenantIdAndDeletedFalse(supplierId, tenantId);
    if (optionalSupplier.isEmpty()){
      log.error("Supplier with id="+supplierId+" having "+tenantId+" not found!");
      throw new SupplierNotFoundException("Supplier with id="+supplierId+" having "+tenantId+" not found!");
    }
    return optionalSupplier.get();
  }

  public Supplier getSupplierByNameAndTenantId(String supplierName, long tenantId){
    Optional<Supplier> optionalSupplier = supplierRepository.findBySupplierNameAndTenantIdAndDeletedFalse(supplierName, tenantId);
    if (optionalSupplier.isEmpty()){
      log.error("Supplier with name="+supplierName+" having "+tenantId+" not found!");
      throw new RuntimeException("Supplier with id="+supplierName+" having "+tenantId+" not found!");
    }
    return optionalSupplier.get();
  }

  public List<Supplier> getSuppliersByTenantId(long tenantId){
    List<Supplier> suppliers = supplierRepository.findByTenantIdAndDeletedFalse(tenantId);
    if (suppliers == null || suppliers.isEmpty()){
      log.error("Suppliers for the tenant= "+tenantId+" not found!");
      throw new RuntimeException("Suppliers for the tenant= "+tenantId+" not found!");
    }
    return suppliers;
  }

  @Transactional
  public void deleteSupplier(long tenantId, long supplierId) {
    // Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // Validate supplier exists and belongs to the tenant
    Supplier supplier = getSupplierByIdAndTenantId(supplierId, tenantId);

    // Check if supplier is associated with any products
    List<Product> associatedProducts = productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
    if (!associatedProducts.isEmpty()) {
        throw new IllegalStateException("Cannot delete supplier as it is associated with products");
    }

    // Perform soft delete
    supplier.setDeleted(true);
    supplier.setDeletedAt(LocalDateTime.now());
    supplierRepository.save(supplier);
  }

  public Supplier getSupplierById(long supplierId){
    Optional<Supplier> supplierOptional = supplierRepository.findByIdAndDeletedFalse(supplierId);
    if (supplierOptional.isEmpty()) {
      throw new SupplierNotFoundException("Supplier not found with supplierId="+supplierId);
    }
    return supplierOptional.get();
  }

  public boolean isSupplierExists(long supplierId){
    Optional<Supplier> supplierOptional = supplierRepository.findByIdAndDeletedFalse(supplierId);
    if (supplierOptional.isEmpty()) {
      log.error("Supplier not found with supplierId = "+supplierId);
      return false;
    }
    return true;
  }

}
