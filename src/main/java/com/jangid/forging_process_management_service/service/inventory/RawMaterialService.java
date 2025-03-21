package com.jangid.forging_process_management_service.service.inventory;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialAssembler;
import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialProductAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.repositories.forging.ForgeHeatRepository;
import com.jangid.forging_process_management_service.repositories.inventory.RawMaterialRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.product.ProductService;
import com.jangid.forging_process_management_service.service.product.SupplierService;
import com.jangid.forging_process_management_service.utils.ConstantUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

  public RawMaterialRepresentation addRawMaterial(Long tenantId, RawMaterialRepresentation rawMaterialRepresentation) {
    if (!isRawMaterialTotalQuantityEqualToHeatsQuantity(rawMaterialRepresentation)) {
      log.error("Total quantity is not equal to the sum of heat quantities!");
      throw new RuntimeException("Total quantity is not equal to the sum of heat quantities!");
    }

    Tenant tenant = tenantService.getTenantById(tenantId);
    RawMaterial rawMaterial = rawMaterialAssembler.createAssemble(rawMaterialRepresentation);
    rawMaterial.setCreatedAt(LocalDateTime.now());
    rawMaterial.setTenant(tenant);
    rawMaterial.setSupplier(supplierService.getSupplierByNameAndTenantId(rawMaterialRepresentation.getSupplier().getSupplierName(), tenantId));

    RawMaterial savedRawMaterial = saveRawMaterial(rawMaterial);
    return rawMaterialAssembler.dissemble(savedRawMaterial);
  }

  @Transactional
  public RawMaterial saveRawMaterial(RawMaterial rawMaterial) {
    return rawMaterialRepository.save(rawMaterial);
  }

  public RawMaterialRepresentation updateRawMaterial(Long tenantId, Long rawMaterialId, RawMaterialRepresentation rawMaterialRepresentation) {

    if (!isRawMaterialTotalQuantityEqualToHeatsQuantity(rawMaterialRepresentation)) {
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
//    List<Heat> heats = rawMaterialHeatRepository.findByHeatNumberAndDeletedIsFalse(heatNumber);
//    if (heats == null){
//      log.error("rawMaterialHeat with heatNumber= "+heatNumber+" for tenant= "+tenantId+" not found!");
//      return Collections.emptyList();
//    }
//    List<RawMaterial> rawMaterials = new ArrayList<>();
//    heats.stream().filter(h -> Objects.equals(tenantId, h.getRawMaterial().getTenant().getId())).forEach(h -> rawMaterials.add(h.getRawMaterial()));
//    return rawMaterials.stream().sorted((a, b) -> b.getRawMaterialReceivingDate().compareTo(a.getRawMaterialReceivingDate())).collect(Collectors.toList());
    return new ArrayList<>();
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

  private boolean areHeatsChangedByHeatQuantity(List<HeatRepresentation> heatRepresentations, List<Heat> existingHeats){
    return !heatRepresentations.stream()
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

  private boolean isRawMaterialTotalQuantityEqualToHeatsQuantity(RawMaterialRepresentation rawMaterialRepresentation) {
    double totalHeatQuantities = rawMaterialRepresentation.getRawMaterialProducts().stream()
        .flatMap(rawMaterialProductRepresentation -> rawMaterialProductRepresentation.getHeats().stream())
        .mapToDouble(heatRepresentation -> Double.parseDouble(heatRepresentation.getHeatQuantity()))
        .sum();

    return Double.compare(totalHeatQuantities, Double.parseDouble(rawMaterialRepresentation.getRawMaterialTotalQuantity())) == 0;
  }

}
