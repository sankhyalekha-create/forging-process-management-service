package com.jangid.forging_process_management_service.resource.inventory;//package com.jangid.forging_process_management_service.resource;
//
//import com.jangid.forging_process_management_service.entities.RawMaterial;
//import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
//import com.jangid.forging_process_management_service.service.RawMaterialService;
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
//  private final RawMaterialService rawMaterialService;
//
//  @GetMapping("/hello")
//  public String getHello() {
//    return "Hello, World!";
//  }
//
//  @GetMapping("/v1/rawMaterial/{id}")
//  public ResponseEntity<RawMaterial> getRawMaterialById(
//      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("id") String id) {
//    Long rawMaterialId = ResourceUtils.convertIdToLong(id)
//        .orElseThrow(() -> new RuntimeException("Not valid id!"));
//
//    RawMaterial rawMaterial = rawMaterialService.getRawMaterialById(Long.valueOf(id));
//    return ResponseEntity.ok(rawMaterial);
//  }
//
//  @GetMapping("/getRawMaterials/{tenantId}")
//  public ResponseEntity<List<RawMaterial>> getAllRawMaterialsByTenantId(
//      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {
//    Long applicationId = ResourceUtils.convertIdToLong(tenantId)
//        .orElseThrow(() -> new TenantNotFoundException(tenantId));
//
//    List<RawMaterial> rawMaterials = rawMaterialService.getAllRawMaterialsOfTenant(Long.valueOf(tenantId));
//    return ResponseEntity.ok(rawMaterials);
//  }
//
//  @PostMapping("/add-raw-material")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public ResponseEntity<RawMaterial> addRawMaterial(@Validated @RequestBody RawMaterial rawMaterial) {
//    try {
//      if (rawMaterial.getRawMaterialInvoiceNumber() == null ||
//          rawMaterial.getRawMaterialReceivingDate() == null ||
//          rawMaterial.getRawMaterialInputCode() == null ||
//          rawMaterial.getRawMaterialTotalQuantity() == 0 ||
//          rawMaterial.getRawMaterialHsnCode() == null ||
//          rawMaterial.getHeats() == null || rawMaterial.getHeats().isEmpty()) {
//        log.error("invalid input!");
//        throw new RuntimeException("invalid input!");
//      }
//      RawMaterial createdRawMaterial = rawMaterialService.addRawMaterial(rawMaterial);
//      return new ResponseEntity<>(createdRawMaterial, HttpStatus.CREATED);
//    } catch (Exception exception) {
//      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//
//  }
//
//}
