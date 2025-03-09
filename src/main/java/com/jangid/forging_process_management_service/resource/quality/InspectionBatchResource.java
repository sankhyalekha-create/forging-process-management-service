package com.jangid.forging_process_management_service.resource.quality;

import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.quality.InspectionBatchNotFoundException;
import com.jangid.forging_process_management_service.service.quality.InspectionBatchService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class InspectionBatchResource {

  @Autowired
  private final InspectionBatchService inspectionBatchService;

  @PostMapping("tenant/{tenantId}/create-inspection-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<InspectionBatchRepresentation> createInspectionBatch(
      @PathVariable String tenantId,
      @RequestBody InspectionBatchRepresentation inspectionBatchRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() ||
          isInvalidInspectionBatchDetails(inspectionBatchRepresentation)) {
        log.error("Invalid createInspectionBatch input!");
        throw new RuntimeException("Invalid createInspectionBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      InspectionBatchRepresentation createdInspectionBatch = inspectionBatchService.createInspectionBatch(
          tenantIdLongValue, inspectionBatchRepresentation);

      return new ResponseEntity<>(createdInspectionBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof InspectionBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/inspection-batches")
  public ResponseEntity<?> getAllInspectionBatchesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                @RequestParam(value = "page", required = false) String page,
                                                @RequestParam(value = "size", required = false) String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      InspectionBatchListRepresentation inspectionBatchListRepresentation = inspectionBatchService.getAllInspectionBatchesOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(inspectionBatchListRepresentation);
    }
    Page<InspectionBatchRepresentation> inspectionBatchRepresentations = inspectionBatchService.getAllInspectionBatchesOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(inspectionBatchRepresentations);
  }




  private boolean isInvalidInspectionBatchDetails(InspectionBatchRepresentation inspectionBatchRepresentation){
    if(inspectionBatchRepresentation == null ||
       inspectionBatchRepresentation.getInspectionBatchNumber()==null || inspectionBatchRepresentation.getInspectionBatchNumber().isEmpty() ||
       inspectionBatchRepresentation.getProcessedItemMachiningBatch()==null ||
       inspectionBatchRepresentation.getProcessedItemMachiningBatch().getAvailableInspectionBatchPiecesCount()==null || inspectionBatchRepresentation.getProcessedItemMachiningBatch().getAvailableInspectionBatchPiecesCount()==0 ||
       inspectionBatchRepresentation.getStartAt()==null || inspectionBatchRepresentation.getStartAt().isEmpty() ||
       inspectionBatchRepresentation.getEndAt()==null || inspectionBatchRepresentation.getEndAt().isEmpty() ||
       inspectionBatchRepresentation.getProcessedItemInspectionBatch().getGaugeInspectionReports()== null || inspectionBatchRepresentation.getProcessedItemInspectionBatch().getGaugeInspectionReports()
           .isEmpty()){
      return true;
    }
    return false;
  }
}
