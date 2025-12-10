package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.ProductAssembler;
import com.jangid.forging_process_management_service.assemblers.product.SupplierAssembler;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.dto.ProductWithHeatsDTO;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
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
import com.jangid.forging_process_management_service.service.document.DocumentService;
import com.jangid.forging_process_management_service.utils.ConstantUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import com.jangid.forging_process_management_service.exception.document.DocumentDeletionException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

  @Autowired
  private DocumentService documentService;

  @Transactional
  public ProductRepresentation createProduct(long tenantId, ProductRepresentation productRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    
    // Check if an active product with same name or code already exists
    boolean existsByNameNotDeleted = productRepository.existsByProductNameAndTenantIdAndDeletedFalse(
        productRepresentation.getProductName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active product with name: {} already exists for tenant: {}!", 
                productRepresentation.getProductName(), tenantId);
      throw new IllegalStateException("Product with name=" + productRepresentation.getProductName() 
                                    + " already exists");
    }
    
    boolean existsByCodeNotDeleted = productRepository.existsByProductCodeAndTenantIdAndDeletedFalse(
        productRepresentation.getProductCode(), tenantId);
    if (existsByCodeNotDeleted) {
      log.error("Active product with code: {} already exists for tenant: {}!", 
                productRepresentation.getProductCode(), tenantId);
      throw new IllegalStateException("Product with code=" + productRepresentation.getProductCode() 
                                    + " already exists");
    }
    
    // Validate suppliers belong to the tenant
    List<Supplier> validSuppliers = productRepresentation.getSuppliers().stream()
        .filter(supplier -> supplier.getTenantId() == tenant.getId() && supplierService.isSupplierExists(supplier.getId()))
        .map(SupplierAssembler::assemble)
        .toList();

    if (validSuppliers.size() != productRepresentation.getSuppliers().size()) {
      throw new SupplierNotFoundException("Some suppliers do not belong to tenant ID: " + tenantId);
    }
    
    // Check if we're trying to revive a deleted product
    Product product = null;
    Optional<Product> deletedProductByName = productRepository.findByProductNameAndTenantIdAndDeletedTrue(
        productRepresentation.getProductName(), tenantId);
    
    if (deletedProductByName.isPresent()) {
      // We found a deleted product with the same name, reactivate it
      log.info("Reactivating previously deleted product with name: {}", productRepresentation.getProductName());
      product = deletedProductByName.get();
      product.setDeleted(false);
      product.setDeletedAt(null);
      
      // Update product fields from the representation
      if (productRepresentation.getProductCode() != null) {
        product.setProductCode(productRepresentation.getProductCode());
      }
      
      if (productRepresentation.getUnitOfMeasurement() != null) {
        product.setUnitOfMeasurement(UnitOfMeasurement.valueOf(productRepresentation.getUnitOfMeasurement()));
      }
    } else {
      // Check for deleted product with same code
      Optional<Product> deletedProductByCode = productRepository.findByProductCodeAndTenantIdAndDeletedTrue(
          productRepresentation.getProductCode(), tenantId);
          
      if (deletedProductByCode.isPresent()) {
        // We found a deleted product with the same code, reactivate it
        log.info("Reactivating previously deleted product with code: {}", productRepresentation.getProductCode());
        product = deletedProductByCode.get();
        product.setDeleted(false);
        product.setDeletedAt(null);
        
        // Update product name and other fields from the representation
        if (productRepresentation.getProductName() != null) {
          product.setProductName(productRepresentation.getProductName());
        }
        
        if (productRepresentation.getUnitOfMeasurement() != null) {
          product.setUnitOfMeasurement(UnitOfMeasurement.valueOf(productRepresentation.getUnitOfMeasurement()));
        }
      } else {
        // Create new product
        product = ProductAssembler.assemble(productRepresentation);
        product.setTenant(tenant);
        product.setCreatedAt(LocalDateTime.now());
      }
    }
    
    // Update suppliers
    List<Supplier> suppliers = productRepresentation.getSuppliers().stream()
        .map(supplierRepresentation -> supplierService.getSupplierById(supplierRepresentation.getId()))
        .collect(Collectors.toList());
    product.setSuppliers(suppliers);
    
    Product savedProduct = saveProduct(product);
    return ProductAssembler.dissemble(savedProduct);
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
        .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
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

    // Check if product is associated with any active ItemProduct
    List<ItemProduct> activeItemProducts = existingProduct.getItemProducts().stream()
        .filter(itemProduct -> !itemProduct.isDeleted())
        .toList();
    
    boolean isFullyEditable = activeItemProducts.isEmpty();

    // If product is associated with active items, only allow adding new suppliers
    if (!isFullyEditable) {
      // Validate that product name, code, and unit of measurement are not being changed
      if (!existingProduct.getProductName().equals(productRepresentation.getProductName())) {
        throw new IllegalStateException("Cannot update product name as it is associated with active items");
      }
      if (!existingProduct.getProductCode().equals(productRepresentation.getProductCode())) {
        throw new IllegalStateException("Cannot update product code as it is associated with active items");
      }
      if (!existingProduct.getUnitOfMeasurement().name().equals(productRepresentation.getUnitOfMeasurement())) {
        throw new IllegalStateException("Cannot update unit of measurement as it is associated with active items");
      }
      
      // Check that we're only adding suppliers, not removing any existing ones
      List<Long> existingSupplierIds = existingProduct.getSuppliers().stream()
          .map(Supplier::getId)
          .toList();
      
      List<Long> inputSupplierIds = productRepresentation.getSuppliers().stream()
          .map(SupplierRepresentation::getId)
          .toList();
      
      // All existing suppliers must be present in the new list
      if (!inputSupplierIds.containsAll(existingSupplierIds)) {
        throw new IllegalStateException("Cannot remove suppliers as product is associated with active items. You can only add new suppliers.");
      }
      
      // Update suppliers (only additions allowed)
      List<Supplier> suppliers = productRepresentation.getSuppliers().stream()
          .map(supplierRepresentation -> supplierService.getSupplierById(supplierRepresentation.getId()))
          .collect(Collectors.toList());
      existingProduct.setSuppliers(suppliers);
    } else {
      // Product is fully editable - allow all changes
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
  public void deleteProduct(Long productId, Long tenantId) throws DocumentDeletionException {
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

    // Delete all documents attached to this product using bulk delete for efficiency
    try {
        // Use bulk delete method from DocumentService for better performance
        documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.PRODUCT, productId);
        log.info("Successfully bulk deleted all documents attached to product {} for tenant {}", productId, tenantId);
    } catch (DataAccessException e) {
        log.error("Database error while deleting documents attached to product {}: {}", productId, e.getMessage(), e);
        throw new DocumentDeletionException("Database error occurred while deleting attached documents for product " + productId, e);
    } catch (RuntimeException e) {
        // Handle document service specific runtime exceptions (storage, file system errors, etc.)
        log.error("Document service error while deleting documents attached to product {}: {}", productId, e.getMessage(), e);
        throw new DocumentDeletionException("Document service error occurred while deleting attached documents for product " + productId + ": " + e.getMessage(), e);
    } catch (Exception e) {
        // Handle any other unexpected exceptions
        log.error("Unexpected error while deleting documents attached to product {}: {}", productId, e.getMessage(), e);
        throw new DocumentDeletionException("Unexpected error occurred while deleting attached documents for product " + productId, e);
    }

    // Soft delete the product
    product.getSuppliers().clear(); // Remove all supplier associations
    product.setDeleted(true);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
    log.info("Successfully deleted product with id={} and all associated documents for tenant={}", productId, tenantId);
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

  public Page<ProductQuantityRepresentation> getProductQuantities(long tenantId, PageRequest pageRequest) {
    List<Object[]> results = productRepository.findProductQuantitiesNative(tenantId);
    List<ProductQuantityRepresentation> productQuantities = results.stream()
        .map(result -> new ProductQuantityRepresentation(
            (String) result[0],                       // productName
            ((Number) result[2]).doubleValue(),      // totalQuantity (index 2 from native query)
            UnitOfMeasurement.valueOf((String) result[1]), // unitOfMeasurement (index 1 from native query)
            ((Number) result[3]).intValue()          // totalPieces (index 3 from native query)
        ))
        .collect(Collectors.toList());

    int start = (int) pageRequest.getOffset();
    int end = Math.min((start + pageRequest.getPageSize()), productQuantities.size());
    List<ProductQuantityRepresentation> pagedList = productQuantities.subList(start, end);

    return new PageImpl<>(pagedList, pageRequest, productQuantities.size());
  }

  // New method to get products with associated heats (paginated)
  public Page<ProductWithHeatsDTO> findProductsWithHeats(long tenantId, int page, int size) {
    tenantService.validateTenantExists(tenantId);

    // Default page size to 5 if not provided or invalid
    int pageSize = (size <= 0) ? 5 : size;
    Pageable pageable = PageRequest.of(page, pageSize);

    // 1. Fetch all non-deleted products for the tenant
    List<Product> allTenantProducts = productRepository.findByTenantIdAndDeletedFalse(tenantId);

    // 2. Filter products that have heats and map to DTOs
    List<ProductWithHeatsDTO> productsWithHeats = allTenantProducts.stream()
        .map(product -> {
            List<HeatInfoDTO> heats = productRepository.findHeatsByProductId(product.getId());
            if (heats != null && !heats.isEmpty()) {
                return new ProductWithHeatsDTO(
                    product.getId(),
                    product.getProductName(),
                    product.getProductCode(),
                    heats
                );
            }
            return null; // Return null for products with no heats
        })
        .filter(dto -> dto != null) // Filter out the nulls (products with no heats)
        .collect(Collectors.toList());

    // 3. Apply pagination to the list of products with heats
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), productsWithHeats.size());
    List<ProductWithHeatsDTO> paginatedProductWithHeatsList = productsWithHeats.subList(start, end);

    // 4. Return a new Page object with the DTOs and the correct total count
    return new PageImpl<>(paginatedProductWithHeatsList, pageable, productsWithHeats.size());
  }

  /**
   * Search for products by product name or product code substring with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (PRODUCT_NAME or PRODUCT_CODE)
   * @param searchTerm The search term (substring matching)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of ProductRepresentation containing the search results
   */
  public Page<ProductRepresentation> searchProducts(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    Pageable pageable = PageRequest.of(page, size);
    Page<Product> productsPage;
    
    switch (searchType.toUpperCase()) {
      case "PRODUCT_NAME":
        productsPage = productRepository.findProductsByProductNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "PRODUCT_CODE":
        productsPage = productRepository.findProductsByProductCodeContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: PRODUCT_NAME, PRODUCT_CODE");
    }
    
    return productsPage.map(ProductAssembler::dissemble);
  }

  /**
   * Check if a product can be fully edited or only suppliers can be added
   * @param productId The product ID
   * @param tenantId The tenant ID
   * @return true if product can be fully edited, false if only suppliers can be added
   */
  public boolean isProductFullyEditable(Long productId, Long tenantId) {
    Product product = getProductById(productId);
    
    // Validate product belongs to tenant
    if (product.getTenant().getId() != tenantId) {
      throw new IllegalStateException("Product does not belong to tenant with id=" + tenantId);
    }
    
    // Check if product is associated with any active ItemProduct
    List<ItemProduct> activeItemProducts = product.getItemProducts().stream()
        .filter(itemProduct -> !itemProduct.isDeleted())
        .toList();
    
    // If no active item products, product can be fully edited
    return activeItemProducts.isEmpty();
  }
}
