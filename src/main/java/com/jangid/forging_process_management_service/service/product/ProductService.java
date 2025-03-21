package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.ProductAssembler;
import com.jangid.forging_process_management_service.assemblers.product.SupplierAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.Supplier;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.ProductQuantityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.product.SupplierNotFoundException;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialProductRepository;
import com.jangid.forging_process_management_service.repositories.product.ProductRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ConstantUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductService {

  @Autowired
  private ProductRepository productRepository;
  @Autowired
  private RawMaterialProductRepository rawMaterialProductRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private SupplierService supplierService;

  @Transactional
  public ProductRepresentation createProduct(long tenantId, ProductRepresentation productRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    // Fetch and validate suppliers, then collect valid suppliers
    List<Supplier> validSuppliers = productRepresentation.getSuppliers().stream()
        .filter(supplier -> supplier.getTenantId() == tenant.getId() && supplierService.isSupplierExists(supplier.getId())).map(SupplierAssembler::assemble)
        .toList();

    if (validSuppliers.size() != productRepresentation.getSuppliers().size()) {
      throw new SupplierNotFoundException("Some suppliers do not belong to tenant ID: " + tenantId);
    }

    Product product = ProductAssembler.assemble(productRepresentation);
    List<Supplier> suppliers = productRepresentation.getSuppliers().stream()
        .map(supplierRepresentation -> supplierService.getSupplierById(supplierRepresentation.getId()))
        .collect(Collectors.toList());
    product.setSuppliers(suppliers);
    product.setTenant(tenant);
    product.setCreatedAt(LocalDateTime.now());
    Product createdProduct = productRepository.save(product);
    return ProductAssembler.dissemble(createdProduct);
  }

  public ProductListRepresentation getAllProductRepresentationsOfSupplier(long tenantId, long supplierId) {
    List<Product> products = productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
    return ProductListRepresentation.builder().productRepresentations(products.stream().map(ProductAssembler::dissemble).collect(Collectors.toList())).build();
  }

  public Page<ProductRepresentation> getAllProductsOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    List<Product> tenantProducts = getTenantProducts(tenantId);

    List<Product> distinctProducts = tenantProducts.stream()
        .filter(ConstantUtils.distinctByKey(Product::getProductCode))
        .toList();
    List<ProductRepresentation> productRepresentations = distinctProducts.stream().map(ProductAssembler::dissemble).collect(Collectors.toList());
    int start = Math.min((int) pageable.getOffset(), productRepresentations.size());
    int end = Math.min((start + pageable.getPageSize()), productRepresentations.size());
    List<ProductRepresentation> pagedList = productRepresentations.subList(start, end);

    return new PageImpl<>(pagedList, pageable, productRepresentations.size());
  }

  public ProductListRepresentation getAllDistinctProductsOfTenantWithoutPagination(long tenantId) {
    List<Product> tenantProducts = getTenantProducts(tenantId);

    List<Product> distinctProducts = tenantProducts.stream()
        .filter(ConstantUtils.distinctByKey(Product::getProductCode))
        .toList();

    List<ProductRepresentation> productRepresentations = distinctProducts.stream().map(ProductAssembler::dissemble).collect(Collectors.toList());
    return ProductListRepresentation.builder().productRepresentations(productRepresentations).build();
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
  public void deleteProduct(Long productId, Long tenantId) {
    // Validate tenant
    tenantService.isTenantExists(tenantId);

    // Validate product exists and belongs to tenant
    Product product = getProductById(productId);
    if (product.getTenant().getId() != tenantId) {
        throw new IllegalStateException("Product does not belong to tenant with id=" + tenantId);
    }

    // Check if product is used in any active ItemProduct
    List<ItemProduct> activeItemProducts = product.getItemProducts().stream()
        .filter(itemProduct -> !itemProduct.isDeleted())
        .toList();
    if (!activeItemProducts.isEmpty()) {
        throw new IllegalStateException("Cannot delete product as it is associated with active items");
    }

    // Check if product is used in any RawMaterialProduct
    List<RawMaterialProduct> rawMaterialProducts = rawMaterialProductRepository.findByProductAndDeletedFalse(product);
    if (!rawMaterialProducts.isEmpty()) {
        throw new IllegalStateException("Cannot delete product as it is associated with raw materials");
    }

    // Soft delete the product
    product.getSuppliers().clear(); // Remove all supplier associations
    product.setDeleted(true);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
  }

  @Transactional
  public Product saveProduct(Product product){
    return productRepository.save(product);
  }

  public List<Product> getTenantProducts(long tenantId){
    List<Supplier> tenantSuppliers = supplierService.getSuppliersByTenantId(tenantId);
    List<Product> tenantProducts = new ArrayList<>();
    tenantSuppliers.forEach(supplier -> {
      List<Product> products = productRepository.findAllBySupplierAndTenant(tenantId, supplier.getId());
      tenantProducts.addAll(products);
    });
    return tenantProducts;
  }

  public List<ProductQuantityRepresentation> getProductQuantities(long tenantId) {
    List<Object[]> results = productRepository.findProductQuantitiesNative(tenantId);
    return results.stream()
        .map(result -> new ProductQuantityRepresentation((String) result[0], (Double) result[1]))
        .collect(Collectors.toList());
  }

}
