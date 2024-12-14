package com.jangid.forging_process_management_service.assemblers;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProcessedItemAssembler {
  @Autowired
  private ForgeAssembler forgeAssembler;
  @Autowired
  private ItemAssembler itemAssembler;

  public ProcessedItemRepresentation dissemble(ProcessedItem processedItem){
    return ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
        .forge(forgeAssembler.dissemble(processedItem.getForge()))
        .item(itemAssembler.dissemble(processedItem.getItem()))
        .itemStatus(processedItem.getItemStatus().name())
        .expectedForgePiecesCount(String.valueOf(processedItem.getExpectedForgePiecesCount()))
        .actualForgePiecesCount(String.valueOf(processedItem.getActualForgePiecesCount()))
        .build();
  }

  public ProcessedItem assemble(ProcessedItemRepresentation processedItemRepresentation){
    return ProcessedItem.builder()
        .forge(forgeAssembler.assemble(processedItemRepresentation.getForge()))
        .item(itemAssembler.assemble(processedItemRepresentation.getItem()))
        .expectedForgePiecesCount(Integer.valueOf(processedItemRepresentation.getExpectedForgePiecesCount()))
        .actualForgePiecesCount(Integer.valueOf(processedItemRepresentation.getActualForgePiecesCount()))
        .itemStatus(ItemStatus.valueOf(processedItemRepresentation.getItemStatus()))
        .build();
  }
}
