package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeTraceabilityRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ForgeTraceabilityAssembler {

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  public ForgeTraceabilityRepresentation dissemble(ForgeTraceability forgeTraceability){
    Heat heat = rawMaterialHeatService.getRawMaterialHeatById(forgeTraceability.getHeatId());
    return ForgeTraceabilityRepresentation.builder()
        .id(forgeTraceability.getId())
        .heatNumber(heat.getHeatNumber())
        .invoiceNumber(heat.getRawMaterial().getRawMaterialInvoiceNumber())
        .heatIdQuantityUsed(forgeTraceability.getHeatIdQuantityUsed().toString())
        .startAt(forgeTraceability.getStartAt()!= null?forgeTraceability.getStartAt().toString():null)
        .endAt(forgeTraceability.getEndAt()!=null?forgeTraceability.getEndAt().toString():null)
        .forgingLineName(forgeTraceability.getForgingLine().getForgingLineName())
        .forgePieceWeight(forgeTraceability.getForgePieceWeight().toString())
        .actualForgeCount(forgeTraceability.getActualForgeCount())
        .forgingStatus(forgeTraceability.getForgingStatus().name())
        .forgingLineName(forgeTraceability.getForgingLine().getForgingLineName())
        .forgePieceWeight(String.valueOf(forgeTraceability.getForgePieceWeight()))
        .build();
  }

  public ForgeTraceability assemble(ForgeTraceabilityRepresentation representation){
    return ForgeTraceability.builder()
        .forgingStatus(ForgeTraceability.ForgeTraceabilityStatus.IDLE)
        .heatIdQuantityUsed(Float.valueOf(representation.getHeatIdQuantityUsed()))
        .forgePieceWeight(Float.valueOf(representation.getForgePieceWeight()))
        .build();
  }

}
