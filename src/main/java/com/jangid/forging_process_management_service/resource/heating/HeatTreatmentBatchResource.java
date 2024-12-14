//package com.jangid.forging_process_management_service.resource.heating;
//
//import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
//import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
//import com.jangid.forging_process_management_service.service.heating.HeatTreatmentBatchService;
//import com.jangid.forging_process_management_service.utils.ResourceUtils;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import jakarta.ws.rs.Consumes;
//import jakarta.ws.rs.Produces;
//import jakarta.ws.rs.core.MediaType;
//
//@Slf4j
//@RestController
//@RequestMapping("/api")
//@RequiredArgsConstructor
//@Component
//public class HeatTreatmentBatchResource {
//
//  @Autowired
//  private HeatTreatmentBatchService heatTreatmentBatchService;
//
//  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}/heat")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public ResponseEntity<HeatTreatmentBatchRepresentation> applyHeatTreatmentBatch(@PathVariable String tenantId, @PathVariable String furnaceId,
//                                                                      @RequestBody HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
//    try {
//      if (furnaceId == null || furnaceId.isEmpty() || tenantId == null || tenantId.isEmpty() || isInvalidHeatTreatmentBatchDetails(heatTreatmentBatchRepresentation)) {
//        log.error("invalid heatTreatmentBatch input for applyHeatTreatmentBatch!");
//        throw new RuntimeException("invalid heatTreatmentBatch input for applyHeatTreatmentBatch!");
//      }
//      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
//          .orElseThrow(() -> new RuntimeException("Not valid tenantId for applyHeatTreatmentBatch!"));
//      Long furnaceIdLongValue = ResourceUtils.convertIdToLong(furnaceId)
//          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for applyHeatTreatmentBatch!"));
//      HeatTreatmentBatchRepresentation createdHeatTreatmentBatch = heatTreatmentBatchService.applyHeatTreatmentBatch(tenantIdLongValue, furnaceIdLongValue, heatTreatmentBatchRepresentation);
//      return new ResponseEntity<>(createdHeatTreatmentBatch, HttpStatus.CREATED);
//    } catch (Exception exception) {
//      if (exception instanceof HeatTreatmentBatchNotFoundException) {
//        return ResponseEntity.notFound().build();
//      }
//      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//  }
//
//  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}/heat/{heatTreatmentBatchId}/startHeatTreatmentBatch")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public ResponseEntity<HeatTreatmentBatchRepresentation> startHeatTreatmentBatch(@PathVariable String tenantId, @PathVariable String furnaceId, @PathVariable String heatTreatmentBatchId,
//                                                                    @RequestBody HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
//    try {
//      if (furnaceId == null || furnaceId.isEmpty() || tenantId == null || tenantId.isEmpty() || heatTreatmentBatchId == null || heatTreatmentBatchId.isEmpty() || heatTreatmentBatchRepresentation.getStartAt() == null
//          || heatTreatmentBatchRepresentation.getStartAt().isEmpty()) {
//        log.error("invalid HeatTreatmentBatch input!");
//        throw new RuntimeException("invalid HeatTreatmentBatch input for startHeatTreatmentBatch!");
//      }
//      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
//          .orElseThrow(() -> new RuntimeException("Not valid tenantId for startHeatTreatmentBatch!"));
//      Long furnaceIdLongValue = ResourceUtils.convertIdToLong(furnaceId)
//          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for startHeatTreatmentBatch!"));
//      Long heatTreatmentBatchIdLongValue = ResourceUtils.convertIdToLong(heatTreatmentBatchId)
//          .orElseThrow(() -> new RuntimeException("Not valid heatTreatmentBatchId for startHeatTreatmentBatch!"));
//
//      HeatTreatmentBatchRepresentation updatedHeatTreatmentBatchRepresentation = heatTreatmentBatchService.startHeatTreatmentBatch(tenantIdLongValue, furnaceIdLongValue, heatTreatmentBatchIdLongValue, heatTreatmentBatchRepresentation.getStartAt());
//      return new ResponseEntity<>(updatedHeatTreatmentBatchRepresentation, HttpStatus.ACCEPTED);
//    } catch (Exception exception) {
//      if (exception instanceof HeatTreatmentBatchNotFoundException) {
//        return ResponseEntity.notFound().build();
//      }
//      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//  }
//
//  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}/heat/{heatTreatmentBatchId}/endHeatTreatmentBatch")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public ResponseEntity<HeatTreatmentBatchRepresentation> endHeatTreatmentBatch(@PathVariable String tenantId, @PathVariable String furnaceId, @PathVariable String heatTreatmentBatchId,
//                                                      @RequestBody HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
//    try {
//      if (furnaceId == null || furnaceId.isEmpty() || tenantId == null || tenantId.isEmpty() || heatTreatmentBatchId == null || heatTreatmentBatchId.isEmpty() || heatTreatmentBatchRepresentation.getEndAt() == null
//          || heatTreatmentBatchRepresentation.getEndAt().isEmpty() || isInvalidHeatTreatmentBatchItemDetails(heatTreatmentBatchRepresentation)) {
//        log.error("invalid heatTreatmentBatchRepresentation input for endHeatTreatmentBatch!");
//        throw new RuntimeException("invalid heatTreatmentBatchRepresentation input!");
//      }
//      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
//          .orElseThrow(() -> new RuntimeException("Not valid tenantId for endHeatTreatmentBatch!"));
//      Long furnaceIdLongValue = ResourceUtils.convertIdToLong(furnaceId)
//          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for endHeatTreatmentBatch!"));
//      Long heatTreatmentBatchIdLongValue = ResourceUtils.convertIdToLong(heatTreatmentBatchId)
//          .orElseThrow(() -> new RuntimeException("Not valid heatTreatmentBatchId for endHeatTreatmentBatch!"));
//
//      HeatTreatmentBatchRepresentation updatedHeatTreatmentBatch = heatTreatmentBatchService.endHeatTreatmentBatch(tenantIdLongValue, furnaceIdLongValue, heatTreatmentBatchIdLongValue, heatTreatmentBatchRepresentation);
//      return new ResponseEntity<>(updatedHeatTreatmentBatch, HttpStatus.ACCEPTED);
//    } catch (Exception exception) {
//      if (exception instanceof HeatTreatmentBatchNotFoundException) {
//        return ResponseEntity.notFound().build();
//      }
//      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//  }
//
//  private boolean isInvalidHeatTreatmentBatchDetails(HeatTreatmentBatchRepresentation representation) {
//    if (representation == null ||
//        representation.getBatchItems() == null || representation.getBatchItems().isEmpty() ||
//        representation.getBatchItems().stream().anyMatch(batchItem -> batchItem.getHeatTreatBatchPiecesCount() == null || batchItem.getHeatTreatBatchPiecesCount().isEmpty())) {
//      log.error("invalid heatTreatmentBatch input!");
//      return true;
//    }
//    return false;
//  }
//
//  private boolean isInvalidHeatTreatmentBatchItemDetails(HeatTreatmentBatchRepresentation representation) {
//    if (representation == null ||
//        representation.getBatchItems() == null || representation.getBatchItems().isEmpty() ||
//        representation.getBatchItems().stream().anyMatch(batchItem -> batchItem.getActualHeatTreatBatchPiecesCount() == null || batchItem.getActualHeatTreatBatchPiecesCount().isEmpty())) {
//      log.error("invalid heatTreatmentBatch item input!");
//      return true;
//    }
//    return false;
//  }
//}
