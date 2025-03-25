package com.jangid.forging_process_management_service.resource;

import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.product.SupplierService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class CacheTestController {

  @Autowired
  private SupplierService supplierService;

  @Autowired
  private ItemService itemService;

  @GetMapping("/cache-test/{tenantId}")
  public ResponseEntity<?> testCache(@PathVariable long tenantId) {
    // First call - should hit database
    long startTime1 = System.currentTimeMillis();
    Page<ItemRepresentation> result1 = itemService.getAllItemsOfTenant(tenantId, 0, 10);
    long endTime1 = System.currentTimeMillis();

    // Second call - should hit cache
    long startTime2 = System.currentTimeMillis();
    Page<ItemRepresentation> result2 = itemService.getAllItemsOfTenant(tenantId, 0, 10);
    long endTime2 = System.currentTimeMillis();

    return ResponseEntity.ok(Map.of(
        "firstCallTime", endTime1 - startTime1,
        "secondCallTime", endTime2 - startTime2
    ));
  }
}
