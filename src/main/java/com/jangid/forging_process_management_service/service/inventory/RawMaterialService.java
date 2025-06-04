package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialAssembler;
import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialProductAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ProductAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.ProductWithHeatsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.SearchResultsRepresentation;
import com.jangid.forging_process_management_service.repositories.forging.ForgeHeatRepository;
import com.jangid.forging_process_management_service.repositories.inventory.HeatRepository;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialRepository;
import com.jangid.forging_process_management_service.repositories.product.ProductRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.product.ProductService;
import com.jangid.forging_process_management_service.service.product.SupplierService;
import com.jangid.forging_process_management_service.utils.ConstantUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RawMaterialService {

  @Autowired
  private RawMaterialRepository rawMaterialRepository;

  @Autowired
  private ProductService productService;

  @Autowired
  private SupplierService supplierService;
  @Autowired
  private TenantService tenantService;

  @Autowired
  private RawMaterialAssembler rawMaterialAssembler;
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;
  @Autowired
  private RawMaterialProductAssembler rawMaterialProductAssembler;

  @Autowired
  private ForgeHeatRepository forgeHeatRepository;
  @Autowired
  private HeatRepository heatRepository;

  @Autowired
  private ProductRepository productRepository;
  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Transactional
  public RawMaterialRepresentation addRawMaterial(Long tenantId, RawMaterialRepresentation rawMaterialRepresentation) {
    if (!isRawMaterialQuantitiesEqualToHeatsQuantities(rawMaterialRepresentation)) {
      log.error("Total quantities are not equal to the sum of heat quantities!");
      throw new RuntimeException("Total quantities are not equal to the sum of heat quantities!");
    }

    // First check if an active (not deleted) raw material with the same invoice number exists
    boolean existsByInvoiceNumberNotDeleted = rawMaterialRepository.existsByRawMaterialInvoiceNumberAndTenantIdAndDeletedFalse(
        rawMaterialRepresentation.getRawMaterialInvoiceNumber(), tenantId);
    if (existsByInvoiceNumberNotDeleted) {
      log.error("Active raw material with invoice number: {} already exists for tenant: {}!", 
                rawMaterialRepresentation.getRawMaterialInvoiceNumber(), tenantId);
      throw new IllegalStateException("Raw material with invoice number=" + rawMaterialRepresentation.getRawMaterialInvoiceNumber() 
                                     + " already exists for tenant=" + tenantId);
    }
    
    // Check if we're trying to revive a deleted raw material
    RawMaterial rawMaterial = null;
    Optional<RawMaterial> deletedRawMaterial = rawMaterialRepository.findByRawMaterialInvoiceNumberAndTenantIdAndDeletedTrue(
        rawMaterialRepresentation.getRawMaterialInvoiceNumber(), tenantId);
    
    if (deletedRawMaterial.isPresent()) {
      // We found a deleted raw material with the same invoice number, reactivate it
      log.info("Reactivating previously deleted raw material with invoice number: {}", 
               rawMaterialRepresentation.getRawMaterialInvoiceNumber());
      rawMaterial = deletedRawMaterial.get();
      rawMaterial.setDeleted(false);
      rawMaterial.setDeletedAt(null);
      
      // Update raw material fields from the representation
      rawMaterial.setRawMaterialInvoiceDate(LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialInvoiceDate(), ConstantUtils.DATE_TIME_FORMATTER));
      rawMaterial.setRawMaterialReceivingDate(LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialReceivingDate(), ConstantUtils.DATE_TIME_FORMATTER));
      rawMaterial.setPoNumber(rawMaterialRepresentation.getPoNumber());
      
      if (rawMaterialRepresentation.getUnitOfMeasurement().equals("KGS")) {
        rawMaterial.setRawMaterialTotalQuantity(Double.valueOf(rawMaterialRepresentation.getRawMaterialTotalQuantity()));
        rawMaterial.setUnitOfMeasurement(UnitOfMeasurement.KGS);
      } else if (rawMaterialRepresentation.getUnitOfMeasurement().equals("PIECES")) {
        rawMaterial.setRawMaterialTotalPieces(rawMaterialRepresentation.getRawMaterialTotalPieces());
        rawMaterial.setUnitOfMeasurement(UnitOfMeasurement.PIECES);
      }
      
      rawMaterial.setRawMaterialHsnCode(rawMaterialRepresentation.getRawMaterialHsnCode());
      rawMaterial.setRawMaterialGoodsDescription(rawMaterialRepresentation.getRawMaterialGoodsDescription());
      rawMaterial.setSupplier(supplierService.getSupplierByIdAndTenantId(rawMaterialRepresentation.getSupplier().getId(), tenantId));
      
      // Clear existing products and set new ones
      rawMaterial.getRawMaterialProducts().clear();
      
      // Create new products from the representation
      List<RawMaterialProduct> newProducts = new ArrayList<>();
      for (RawMaterialProductRepresentation productRep : rawMaterialRepresentation.getRawMaterialProducts()) {
        RawMaterialProduct newProduct = rawMaterialProductAssembler.assemble(productRep);
        newProduct.setRawMaterial(rawMaterial);
        newProduct.setDeleted(false);
        newProduct.setDeletedAt(null);
        
        // Set product from product service
        newProduct.setProduct(productService.getProductById(productRep.getProduct().getId()));
        
        newProducts.add(newProduct);
      }
      
      rawMaterial.updateRawMaterialProducts(newProducts);
    } else {
      // Create new raw material
      Tenant tenant = tenantService.getTenantById(tenantId);
      rawMaterial = rawMaterialAssembler.createAssemble(rawMaterialRepresentation);
      rawMaterial.setCreatedAt(LocalDateTime.now());
      rawMaterial.setTenant(tenant);
      rawMaterial.setSupplier(supplierService.getSupplierByIdAndTenantId(rawMaterialRepresentation.getSupplier().getId(), tenantId));
    }

    RawMaterial savedRawMaterial = saveRawMaterial(rawMaterial);
    return rawMaterialAssembler.dissemble(savedRawMaterial);
  }

  @Transactional
  public RawMaterial saveRawMaterial(RawMaterial rawMaterial) {
    return rawMaterialRepository.save(rawMaterial);
  }

  public RawMaterialRepresentation updateRawMaterial(Long tenantId, Long rawMaterialId, RawMaterialRepresentation rawMaterialRepresentation) {

    if (!isRawMaterialQuantitiesEqualToHeatsQuantities(rawMaterialRepresentation)) {
      log.error("Total quantity is not equal to the sum of heat quantities!");
      throw new RuntimeException("Total quantity is not equal to the sum of heat quantities!");
    }

    Tenant tenant = tenantService.getTenantById(tenantId);
    RawMaterial existingRawMaterial = getRawMaterialByIdAndTenantId(rawMaterialId, tenantId);

    existingRawMaterial.setTenant(tenant);

    if (rawMaterialRepresentation.getRawMaterialInvoiceDate() != null && !existingRawMaterial.getRawMaterialInvoiceDate().toString().equals(rawMaterialRepresentation.getRawMaterialInvoiceDate())) {
      existingRawMaterial.setRawMaterialInvoiceDate(LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialInvoiceDate(), ConstantUtils.DATE_TIME_FORMATTER));
    }

    if (rawMaterialRepresentation.getRawMaterialReceivingDate() != null && !existingRawMaterial.getRawMaterialReceivingDate().toString()
        .equals(rawMaterialRepresentation.getRawMaterialReceivingDate())) {
      existingRawMaterial.setRawMaterialReceivingDate(LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialReceivingDate(), ConstantUtils.DATE_TIME_FORMATTER));
    }
    if (!existingRawMaterial.getRawMaterialInvoiceNumber().equals(rawMaterialRepresentation.getRawMaterialInvoiceNumber())) {
      existingRawMaterial.setRawMaterialInvoiceNumber(rawMaterialRepresentation.getRawMaterialInvoiceNumber());
    }

    if (!existingRawMaterial.getPoNumber().equals(rawMaterialRepresentation.getPoNumber())) {
      existingRawMaterial.setPoNumber(rawMaterialRepresentation.getPoNumber());
    }

    if (!String.valueOf(existingRawMaterial.getRawMaterialTotalQuantity()).equals(rawMaterialRepresentation.getRawMaterialTotalQuantity())) {
      existingRawMaterial.setRawMaterialTotalQuantity(Double.valueOf(rawMaterialRepresentation.getRawMaterialTotalQuantity()));
    }
    if (!existingRawMaterial.getRawMaterialHsnCode().equals(rawMaterialRepresentation.getRawMaterialHsnCode())) {
      existingRawMaterial.setRawMaterialHsnCode(rawMaterialRepresentation.getRawMaterialHsnCode());
    }
    if (!existingRawMaterial.getRawMaterialGoodsDescription().equals(rawMaterialRepresentation.getRawMaterialGoodsDescription())) {
      existingRawMaterial.setRawMaterialGoodsDescription(rawMaterialRepresentation.getRawMaterialGoodsDescription());
    }
    if(!existingRawMaterial.getSupplier().getSupplierName().equals(rawMaterialRepresentation.getSupplier().getSupplierName())){
      existingRawMaterial.setSupplier(supplierService.getSupplierByNameAndTenantId(rawMaterialRepresentation.getSupplier().getSupplierName(), tenantId));
    }

    updateRawMaterialProducts(existingRawMaterial, rawMaterialRepresentation.getRawMaterialProducts());
    RawMaterial savedRawMaterial = saveRawMaterial(existingRawMaterial);

    return rawMaterialAssembler.dissemble(savedRawMaterial);
  }

  private void updateRawMaterialProducts(RawMaterial existingRawMaterial, List<RawMaterialProductRepresentation> newProductRepresentations) {
    // Retrieve the existing collection of products
    List<RawMaterialProduct> existingProducts = existingRawMaterial.getRawMaterialProducts();

    // Map for quick lookup of existing products by product ID
    Map<Long, RawMaterialProduct> existingProductMap = existingProducts.stream()
        .collect(Collectors.toMap(rp -> rp.getProduct().getId(), rp -> rp));

    // List for products to be added and removed
    List<RawMaterialProduct> productsToAdd = new ArrayList<>();
    Set<Long> newProductIds = new HashSet<>();

    // Iterate over the new product representations to update existing ones or add new ones
    for (RawMaterialProductRepresentation newProductRep : newProductRepresentations) {
      Long productId = newProductRep.getProduct().getId();
      newProductIds.add(productId); // Track all product IDs in the new representation

      RawMaterialProduct existingProduct = existingProductMap.get(productId);

      if (existingProduct != null) {
        // Update the heats for the existing product if required
        updateHeatsIfRequired(existingProduct, newProductRep);
        existingProduct.setProduct(productService.getProductById(existingProduct.getProduct().getId()));
        existingRawMaterial.setRawMaterial(existingProduct);
      } else {
        // Create and set up a new RawMaterialProduct if it doesn't exist in the existing collection
        RawMaterialProduct newRawMaterialProduct = rawMaterialProductAssembler.assemble(newProductRep);
        newRawMaterialProduct.setProduct(productService.getProductById(productId));
        newRawMaterialProduct.setRawMaterial(existingRawMaterial);
        productsToAdd.add(newRawMaterialProduct);
      }
    }

    // Remove products that are no longer in the new representation
    existingProducts.removeIf(existingProduct -> !newProductIds.contains(existingProduct.getProduct().getId()));

    // Add new products
    existingProducts.addAll(productsToAdd);
  }

  private void updateHeatsIfRequired(RawMaterialProduct existingProduct, RawMaterialProductRepresentation newProductRep) {
    List<HeatRepresentation> newHeats = newProductRep.getHeats();

    if (isAnyChangeToHeatsHappened(newHeats, existingProduct.getHeats())) {
      List<Heat> updatedHeats = rawMaterialHeatAssembler.getHeats(newHeats);

      // Clear the existing heats, but retain the original list instance
      existingProduct.getHeats().clear();

      // Add the new heats to the existing list
      updatedHeats.forEach(heat -> {
        heat.setCreatedAt(LocalDateTime.now());
        heat.setRawMaterialProduct(existingProduct); // Ensure bidirectional mapping is consistent
        existingProduct.getHeats().add(heat);
      });
    }
  }

  public Page<RawMaterialRepresentation> getAllRawMaterialsOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<RawMaterial> rawMaterialsPage = rawMaterialRepository.findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(tenantId, pageable);
    return rawMaterialsPage.map(rawMaterialAssembler::dissemble);
  }

  /**
   * Get all raw materials for a tenant with products and heats eagerly loaded
   * This method optimizes database queries by using JOIN FETCH to load all related data in one query,
   * then manually applies pagination to avoid N+1 query problems.
   * 
   * @param tenantId The tenant ID
   * @param page Page number (0-based)
   * @param size Page size
   * @return Page of RawMaterialRepresentation with products and heats included
   */
  public Page<RawMaterialRepresentation> getAllRawMaterialsOfTenantWithProductsAndHeats(long tenantId, int page, int size) {
    // First get raw materials with products
    List<RawMaterial> allRawMaterials = rawMaterialRepository.findByTenantIdAndDeletedIsFalseWithProductsAndHeats(tenantId);
    
    // Get all raw material IDs
    List<Long> rawMaterialIds = allRawMaterials.stream()
        .map(RawMaterial::getId)
        .toList();
    
    // Load all raw material products with heats in a separate query
    List<RawMaterialProduct> productsWithHeats = rawMaterialRepository.findRawMaterialProductsWithHeatsByRawMaterialIds(rawMaterialIds);
    
    // Create a map of raw material product ID to the loaded product with heats
    Map<Long, RawMaterialProduct> productMap = productsWithHeats.stream()
        .collect(Collectors.toMap(RawMaterialProduct::getId, p -> p));
    
    // Update the heats in the original raw materials
    allRawMaterials.forEach(rm -> 
        rm.getRawMaterialProducts().forEach(rmp -> {
            RawMaterialProduct loadedProduct = productMap.get(rmp.getId());
            if (loadedProduct != null) {
                rmp.setHeats(loadedProduct.getHeats());
            }
        })
    );
    
    // Apply pagination manually
    int start = page * size;
    int end = Math.min(start + size, allRawMaterials.size());
    List<RawMaterial> pagedRawMaterials = start < allRawMaterials.size() ? 
        allRawMaterials.subList(start, end) : 
        Collections.emptyList();
    
    // Convert to representations
    List<RawMaterialRepresentation> representations = pagedRawMaterials.stream()
        .map(rawMaterialAssembler::dissemble)
        .toList();
    
    return new PageImpl<>(
        representations,
        PageRequest.of(page, size),
        allRawMaterials.size()
    );
  }

  public RawMaterialRepresentation getTenantRawMaterialById(long tenantId, long materialId) {
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findByIdAndTenantIdAndDeletedFalse(materialId, tenantId);
    if (optionalRawMaterial.isEmpty()) {
      log.error("RawMaterial with id=" + materialId + " not found!");
      throw new RuntimeException("RawMaterial with id=" + materialId + " not found!");
    }
    RawMaterial rawMaterial = optionalRawMaterial.get();
    return rawMaterialAssembler.dissemble(rawMaterial);
  }

  @Transactional
  public void deleteRawMaterial(Long rawMaterialId, Long tenantId) {
    // 1. Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // 2. Validate raw material exists
    RawMaterial rawMaterial = getRawMaterialByIdAndTenantId(rawMaterialId, tenantId);

    // 3. Validate no heats are in use in forge heats
    List<Heat> heatsInUse = new ArrayList<>();
    for (RawMaterialProduct product : rawMaterial.getRawMaterialProducts()) {
        for (Heat heat : product.getHeats()) {
            // Check if heat is used in any non-deleted forge heat
            boolean isHeatInUse = forgeHeatRepository.existsByHeatIdAndDeletedFalse(heat.getId());
            if (isHeatInUse) {
                heatsInUse.add(heat);
            }
        }
    }

    if (!heatsInUse.isEmpty()) {
        String heatNumbers = heatsInUse.stream()
            .map(Heat::getHeatNumber)
            .collect(Collectors.joining(", "));
        throw new IllegalStateException("Cannot delete raw material as heats [" + heatNumbers + "] are in use in forging process");
    }

    // 4. Soft delete raw material and associated entities
    LocalDateTime now = LocalDateTime.now();

    // Set deleted flag and timestamp for raw material
    rawMaterial.setDeleted(true);
    rawMaterial.setDeletedAt(now);

    // Set deleted flag and timestamp for raw material products
    rawMaterial.getRawMaterialProducts().forEach(rmp -> {
        rmp.setDeleted(true);
        rmp.setDeletedAt(now);

        // Set deleted flag and timestamp for heats
        rmp.getHeats().forEach(heat -> {
            heat.setDeleted(true);
            heat.setDeletedAt(now);
        });
    });

    // Save the updated raw material (cascades to related entities)
    rawMaterialRepository.save(rawMaterial);
    log.info("Successfully deleted raw material with id={} for tenant={}", rawMaterialId, tenantId);
  }

  private RawMaterial getRawMaterialByIdAndTenantId(long materialId, long tenantId) {
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findByIdAndTenantIdAndDeletedFalse(materialId, tenantId);
    if (optionalRawMaterial.isEmpty()) {
      log.error("RawMaterial with id=" + materialId + " having " + tenantId + " not found!");
      throw new RuntimeException("RawMaterial with id=" + materialId + " having " + tenantId + " not found!");
    }
    return optionalRawMaterial.get();
  }

  public RawMaterial getRawMaterialByInvoiceNumber(long tenantId, String invoiceNumber) {
    Optional<RawMaterial> optionalRawMaterial = rawMaterialRepository.findByTenantIdAndRawMaterialInvoiceNumberAndDeletedIsFalse(tenantId, invoiceNumber);
    if (optionalRawMaterial.isEmpty()) {
      log.error("RawMaterial with invoiceNumber=" + invoiceNumber + " for tenant=" + tenantId + " not found!");
      return null;
    }
    return optionalRawMaterial.get();
  }

  public List<RawMaterial> getRawMaterialByHeatNumber(long tenantId, String heatNumber) {
    List<Heat> heats = heatRepository.findByHeatNumberAndDeletedIsFalse(heatNumber);
    if (heats == null){
      log.error("rawMaterialHeat with heatNumber= "+heatNumber+" for tenant= "+tenantId+" not found!");
      return Collections.emptyList();
    }
    List<RawMaterial> rawMaterials = new ArrayList<>();
    heats.stream().filter(h -> Objects.equals(tenantId, h.getRawMaterialProduct().getRawMaterial().getTenant().getId())).forEach(h -> rawMaterials.add(h.getRawMaterialProduct().getRawMaterial()));
    return rawMaterials.stream().sorted((a, b) -> b.getRawMaterialReceivingDate().compareTo(a.getRawMaterialReceivingDate())).collect(Collectors.toList());
  }

  private List<Heat> getHeats(RawMaterial rawMaterial, RawMaterialRepresentation rawMaterialRepresentation) {
    List<Heat> heats = new ArrayList<>();
//    for(RawMaterialHeatRepresentation heat : rawMaterialRepresentation.getHeats()){
//      heats.add(Heat.builder()
//                    .heatNumber(heat.getHeatNumber())
//                    .heatQuantity(Float.valueOf(heat.getHeatQuantity()))
//                    .availableHeatQuantity(Float.valueOf(heat.getHeatQuantity()))
//                    .rawMaterialTestCertificateNumber(heat.getRawMaterialTestCertificateNumber())
//                    .barDiameter(heat.getBarDiameter()!=null ? BarDiameter.valueOf(heat.getBarDiameter()): null)
//                    .rawMaterialReceivingInspectionReportNumber(heat.getRawMaterialReceivingInspectionReportNumber())
//                    .rawMaterialInspectionSource(heat.getRawMaterialInspectionSource())
//                    .rawMaterialLocation(heat.getRawMaterialLocation())
//                    .rawMaterial(rawMaterial).build());
//    }
    return heats;
  }

  public RawMaterialListRepresentation getRawMaterialListRepresentation(List<RawMaterial> rawMaterials) {
    if (rawMaterials == null) {
      log.error("RawMaterial list is null!");
      return RawMaterialListRepresentation.builder().build();
    }
    List<RawMaterialRepresentation> rawMaterialRepresentations = new ArrayList<>();
    rawMaterials.forEach(rm -> rawMaterialRepresentations.add(rawMaterialAssembler.dissemble(rm)));
    return RawMaterialListRepresentation.builder()
        .rawMaterials(rawMaterialRepresentations).build();
  }

  public List<RawMaterial> getRawMaterialByStartAndEndDate(String startDate, String endDate, long tenantId) {
    LocalDateTime sDate = LocalDate.parse(startDate, ConstantUtils.DAY_FORMATTER).atStartOfDay();
    endDate = endDate + ConstantUtils.LAST_MINUTE_OF_DAY;
    LocalDateTime eDate = LocalDateTime.parse(endDate, ConstantUtils.DATE_TIME_FORMATTER);

    List<RawMaterial> rawMaterials = rawMaterialRepository.findByTenantIdAndRawMaterialReceivingDateGreaterThanAndRawMaterialReceivingDateLessThanAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(
        tenantId, sDate, eDate);
    if (rawMaterials == null) {
      log.error("RawMaterials with startDate= " + startDate + " and endDate= " + endDate + " for tenant= " + tenantId + " not found!");
      return Collections.emptyList();
    }
    return rawMaterials;
  }

  public List<Heat> getAvailableRawMaterialByTenantId(long tenantId) {
//    List<RawMaterial> rawMaterials = rawMaterialRepository.findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(tenantId);
//
//    List<Heat> heats = rawMaterials.stream()
//        .flatMap(rm -> rm.getHeats().stream())
//        .filter(rmh -> rmh.getAvailableHeatQuantity() > 0)
//        .collect(Collectors.toList());
//    if (heats.isEmpty()) {
//      log.info("No records exist for tenant={} with heats having available quantity greater than 0", tenantId);
//    }
//
//    return heats;
    return new ArrayList<>();
  }

  private boolean isAnyChangeToHeatsHappened(List<HeatRepresentation> heatRepresentations, List<Heat> existingHeats){
    if (heatRepresentations == null || existingHeats == null) {
      return heatRepresentations == null && existingHeats == null;
    }

    if (heatRepresentations.size()!=existingHeats.size()){
      return true;
    }

    boolean areSameHeatsByHeatNumber = heatRepresentations.stream()
        .map(HeatRepresentation::getHeatNumber)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .equals(
            existingHeats.stream()
                .map(Heat::getHeatNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

    boolean areSameHeatsByHeatQuantity = !areHeatsChangedByHeatQuantity(heatRepresentations, existingHeats);

    boolean areSameHeatsByHeatTestCertificate = heatRepresentations.stream()
        .map(HeatRepresentation::getTestCertificateNumber)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .equals(
            existingHeats.stream()
                .map(Heat::getTestCertificateNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

    boolean areSameHeatsByHeatLocation = heatRepresentations.stream()
        .map(HeatRepresentation::getLocation)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .equals(
            existingHeats.stream()
                .map(Heat::getLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );
    return !areSameHeatsByHeatNumber || !areSameHeatsByHeatQuantity || !areSameHeatsByHeatTestCertificate || !areSameHeatsByHeatLocation;
  }

  private boolean areHeatsChangedByHeatQuantity(List<HeatRepresentation> heatRepresentations, List<Heat> existingHeats) {
    if (heatRepresentations == null || existingHeats == null) {
      return heatRepresentations == null && existingHeats == null;
    }

    // Group heats by isInPieces flag
    Map<Boolean, List<HeatRepresentation>> heatRepsByType = heatRepresentations.stream()
        .collect(Collectors.groupingBy(HeatRepresentation::getIsInPieces));

    Map<Boolean, List<Heat>> existingHeatsByType = existingHeats.stream()
        .collect(Collectors.groupingBy(Heat::getIsInPieces));

    // Compare weight-based heats
    boolean weightBasedHeatsMatch = compareWeightBasedHeats(
        heatRepsByType.getOrDefault(false, Collections.emptyList()),
        existingHeatsByType.getOrDefault(false, Collections.emptyList())
    );

    // Compare piece-based heats
    boolean pieceBasedHeatsMatch = comparePieceBasedHeats(
        heatRepsByType.getOrDefault(true, Collections.emptyList()),
        existingHeatsByType.getOrDefault(true, Collections.emptyList())
    );

    return weightBasedHeatsMatch && pieceBasedHeatsMatch;
  }

  private boolean compareWeightBasedHeats(List<HeatRepresentation> heatReps, List<Heat> existingHeats) {
    return heatReps.stream()
        .map(HeatRepresentation::getHeatQuantity)
        .map(Double::valueOf)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .equals(
            existingHeats.stream()
                .map(Heat::getHeatQuantity)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );
  }

  private boolean comparePieceBasedHeats(List<HeatRepresentation> heatReps, List<Heat> existingHeats) {
    return heatReps.stream()
        .map(HeatRepresentation::getPiecesCount)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .equals(
            existingHeats.stream()
                .map(Heat::getPiecesCount)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );
  }

  private boolean isRawMaterialQuantitiesEqualToHeatsQuantities(RawMaterialRepresentation rawMaterialRepresentation) {
    if (rawMaterialRepresentation.getUnitOfMeasurement().equals("KGS")) {
      // Validate weight-based quantities
      double totalHeatQuantities = rawMaterialRepresentation.getRawMaterialProducts().stream()
          .flatMap(rawMaterialProductRepresentation -> rawMaterialProductRepresentation.getHeats().stream())
          .filter(heat -> !heat.getIsInPieces())
          .mapToDouble(heatRepresentation -> Double.parseDouble(heatRepresentation.getHeatQuantity()))
          .sum();

      return Double.compare(totalHeatQuantities, Double.parseDouble(rawMaterialRepresentation.getRawMaterialTotalQuantity())) == 0;
    } else if (rawMaterialRepresentation.getUnitOfMeasurement().equals("PIECES")) {
      // Validate piece-based quantities
      int totalPieces = rawMaterialRepresentation.getRawMaterialProducts().stream()
          .flatMap(rawMaterialProductRepresentation -> rawMaterialProductRepresentation.getHeats().stream())
          .filter(heat -> heat.getIsInPieces())
          .mapToInt(HeatRepresentation::getPiecesCount)
          .sum();

      return totalPieces == rawMaterialRepresentation.getRawMaterialTotalPieces();
    }
    return false;
  }

  private boolean isRawMaterialTotalQuantityEqualToHeatsQuantity(RawMaterialRepresentation rawMaterialRepresentation) {
    double totalHeatQuantities = rawMaterialRepresentation.getRawMaterialProducts().stream()
        .flatMap(rawMaterialProductRepresentation -> rawMaterialProductRepresentation.getHeats().stream())
        .mapToDouble(heatRepresentation -> Double.parseDouble(heatRepresentation.getHeatQuantity()))
        .sum();

    return Double.compare(totalHeatQuantities, Double.parseDouble(rawMaterialRepresentation.getRawMaterialTotalQuantity())) == 0;
  }

  /**
   * Search for products and heats based on different criteria
   * @param tenantId The tenant ID
   * @param searchType The type of search (PRODUCT_NAME, PRODUCT_CODE, HEAT_NUMBER)
   * @param searchTerm The search term
   * @return SearchResultsRepresentation containing the search results
   */
  public SearchResultsRepresentation searchProductsAndHeats(Long tenantId, String searchType, String searchTerm) {
    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      return SearchResultsRepresentation.builder()
          .searchType(searchType)
          .searchTerm(searchTerm)
          .build();
    }

    SearchResultsRepresentation.SearchResultsRepresentationBuilder builder = SearchResultsRepresentation.builder()
        .searchType(searchType)
        .searchTerm(searchTerm.trim());

    switch (searchType.toUpperCase()) {
      case "PRODUCT_NAME":
        List<Product> productsByName = productRepository.findProductsByProductNameContainingIgnoreCase(tenantId, searchTerm.trim());
        List<ProductWithHeatsRepresentation> productsWithHeatsByName = new ArrayList<>();
        
        for (Product product : productsByName) {
          List<Heat> heats = rawMaterialHeatService.getProductHeats(tenantId, product.getId());
          List<HeatRepresentation> heatRepresentations = heats.stream()
              .map(rawMaterialHeatAssembler::dissemble)
              .collect(Collectors.toList());
          
          ProductWithHeatsRepresentation productWithHeats = ProductWithHeatsRepresentation.builder()
              .product(ProductAssembler.dissemble(product))
              .heats(heatRepresentations)
              .build();
          
          productsWithHeatsByName.add(productWithHeats);
        }
        
        builder.productsWithHeats(productsWithHeatsByName);
        break;

      case "PRODUCT_CODE":
        List<Product> productsByCode = productRepository.findProductsByProductCodeContainingIgnoreCase(tenantId, searchTerm.trim());
        List<ProductWithHeatsRepresentation> productsWithHeatsByCode = new ArrayList<>();
        
        for (Product product : productsByCode) {
          List<Heat> heats = rawMaterialHeatService.getProductHeats(tenantId, product.getId());
          List<HeatRepresentation> heatRepresentations = heats.stream()
              .map(rawMaterialHeatAssembler::dissemble)
              .collect(Collectors.toList());
          
          ProductWithHeatsRepresentation productWithHeats = ProductWithHeatsRepresentation.builder()
              .product(ProductAssembler.dissemble(product))
              .heats(heatRepresentations)
              .build();
          
          productsWithHeatsByCode.add(productWithHeats);
        }
        
        builder.productsWithHeats(productsWithHeatsByCode);
        break;

      case "HEAT_NUMBER":
        List<Heat> heatsByNumber = heatRepository.findHeatsByHeatNumberContainingIgnoreCase(tenantId, searchTerm.trim());
        List<HeatRepresentation> heatRepresentations = heatsByNumber.stream()
            .map(rawMaterialHeatAssembler::dissemble)
            .collect(Collectors.toList());
        
        builder.heats(heatRepresentations);
        break;

      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: PRODUCT_NAME, PRODUCT_CODE, HEAT_NUMBER");
    }

    return builder.build();
  }
}
