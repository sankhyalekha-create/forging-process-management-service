//package com.jangid.forging_process_management_service.resource.inventory;//package com.jangid.forging_process_management_service.resource;
//
//import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialAssembler;
//import com.jangid.forging_process_management_service.entities.RawMaterial;
//import com.jangid.forging_process_management_service.entities.inventory.RawMaterialHeat;
//import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialHeatListRepresentation;
//import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialHeatRepresentation;
//import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialListRepresentation;
//import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialRepresentation;
//import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
//import com.jangid.forging_process_management_service.service.RawMaterialService;
//import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
//import com.jangid.forging_process_management_service.utils.ResourceUtils;
//
//import io.swagger.annotations.ApiParam;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.ArrayList;
//import java.util.List;
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
//public class RawMaterialHeatResource {
//
//  @Autowired
//  private final RawMaterialHeatService rawMaterialHeatService;
//
//  @GetMapping("/tenant/{tenantId}/rawMaterialHeats/available")
//  public ResponseEntity<RawMaterialHeatListRepresentation> getAvailableRawMaterialHeats(
//      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId) {
//    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
//        .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
//
//    List<RawMaterialHeat> heats = rawMaterialHeatService.getTenantsAvailableHeats(tenantIdLongValue);
//    RawMaterialHeatListRepresentation rawMaterialListRepresentation = getRawMaterialHeatListRepresentation(heats);
//    return ResponseEntity.ok(rawMaterialListRepresentation);
//  }
//
//  private RawMaterialHeatListRepresentation getRawMaterialHeatListRepresentation(List<RawMaterialHeat> heats){
//    if (heats == null) {
//      log.error("RawMaterialHeats list is null!");
//      return RawMaterialHeatListRepresentation.builder().build();
//    }
//    List<RawMaterialHeatRepresentation> rawMaterialHeatRepresentations = new ArrayList<>();
//    rawMaterials.forEach(rm -> rawMaterialHeatRepresentations.add(RawMaterialAssembler.dissemble(rm)));
//    return RawMaterialListRepresentation.builder()
//        .rawMaterials(rawMaterialRepresentations).build();
//
//  }
//
//
//}
