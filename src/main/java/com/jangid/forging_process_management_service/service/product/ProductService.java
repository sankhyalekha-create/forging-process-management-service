package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.ProductAssembler;
import com.jangid.forging_process_management_service.assemblers.product.SupplierAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.Supplier;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.product.SupplierNotFoundException;
import com.jangid.forging_process_management_service.repositories.product.ProductRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductService {

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private SupplierService supplierService;

  @Transactional
  public ProductRepresentation createProduct(long tenantId, ProductRepresentation productRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    // Fetch and validate suppliers, then collect valid suppliers
    List<Supplier> validSuppliers = productRepresentation.getSuppliers().stream()
        .filter(supplier -> supplier.getTenantId() == tenant.getId()).map(SupplierAssembler::assemble)
        .toList();

    if (validSuppliers.size() != productRepresentation.getSuppliers().size()) {
      throw new SupplierNotFoundException("Some suppliers do not belong to tenant ID: " + tenantId);
    }

    Product product = ProductAssembler.assemble(productRepresentation);
    List<Supplier> suppliers = productRepresentation.getSuppliers().stream()
        .map(supplierRepresentation -> supplierService.getSupplierById(supplierRepresentation.getId()))
        .collect(Collectors.toList());
    product.setSuppliers(suppliers);
    product.setCreatedAt(LocalDateTime.now());
    Product createdProduct = productRepository.save(product);
    return ProductAssembler.dissemble(createdProduct);
  }

  public ProductListRepresentation getAllProductRepresentationsOfSupplier(long tenantId, long supplierId) {
    List<Product> products = productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
    return ProductListRepresentation.builder().productRepresentations(products.stream().map(ProductAssembler::dissemble).collect(Collectors.toList())).build();
  }

  public List<Product> getAllProductsOfSupplier(long tenantId, long supplierId) {
    return productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
  }


  @Transactional
  public ProductRepresentation updateProduct(long tenantId, long productId, ProductRepresentation productRepresentation) {
    Product existingProduct = getProductById(productId);

    if (!productRepresentation.getSuppliers().stream().allMatch(s -> s.getTenantId() == tenantId)) {
      throw new RuntimeException("Supplier provided in input request is not a valid supplier of tenant="+tenantId);
    }

    if (!existingProduct.getProductName().equals(productRepresentation.getProductName())) {
      existingProduct.setProductName(productRepresentation.getProductName());
    }
    if (!existingProduct.getProductCode().equals(productRepresentation.getProductCode())) {
      existingProduct.setProductCode(productRepresentation.getProductCode());
    }

    if (!existingProduct.getProductSku().equals(productRepresentation.getProductSku())) {
      existingProduct.setProductSku(productRepresentation.getProductSku());
    }

    if (!existingProduct.getUnitOfMeasurement().name().equals(productRepresentation.getUnitOfMeasurement())) {
      existingProduct.setUnitOfMeasurement(UnitOfMeasurement.valueOf(productRepresentation.getUnitOfMeasurement()));
    }

    List<Long> existingSupplierIds = existingProduct.getSuppliers().stream()
        .map(Supplier::getId)
        .toList();

    List<Long> inputSupplierIds = productRepresentation.getSuppliers().stream()
        .map(SupplierRepresentation::getId)
        .toList();

    // If there is a difference in supplier lists, update the suppliers
    if (!existingSupplierIds.containsAll(inputSupplierIds) || existingSupplierIds.size() != inputSupplierIds.size()) {
      List<Supplier> suppliers = productRepresentation.getSuppliers().stream()
          .map(supplierRepresentation -> supplierService.getSupplierById(supplierRepresentation.getId()))
          .collect(Collectors.toList());
      existingProduct.setSuppliers(suppliers);
    }

    Product updatedProduct = productRepository.save(existingProduct);
    return ProductAssembler.dissemble(updatedProduct);
  }

  public Product getProductById(long productId) {
    Optional<Product> optionalProduct = productRepository.findByIdAndDeletedFalse(productId);
    if (optionalProduct.isEmpty()) {
      log.error("Product with id=" + productId + " not found!");
      throw new RuntimeException("Product with id=" + productId + " not found!");
    }
    return optionalProduct.get();
  }

  @Transactional
  public void deleteProductById(Long productId, Long tenantId) {
    Product existingProduct = getProductById(productId);

    if (!existingProduct.getSuppliers().stream().allMatch(s -> s.getTenant().getId() == tenantId)) {
      throw new RuntimeException("Supplier provided in input request is not a valid supplier of tenant="+tenantId);
    }
    Optional<Product> productOptional = productRepository.findByIdAndDeletedFalse(productId);
    if (productOptional.isEmpty()) {
      log.error("product with id=" + productId + " not found!");
      throw new ResourceNotFoundException("product with id=" + productId + " not found!");
    }
    Product product = productOptional.get();
    product.getSuppliers().clear();
    product.setDeleted(true);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
  }

  @Transactional
  public Product saveProduct(Product product){
    return productRepository.save(product);
  }

}
