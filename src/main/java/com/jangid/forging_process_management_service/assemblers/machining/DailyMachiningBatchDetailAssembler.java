package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatchDetail;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchDetailRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
public class DailyMachiningBatchDetailAssembler {

  @Autowired
  private MachiningBatchAssembler machiningBatchAssembler;

  public DailyMachiningBatchDetail createAssemble(DailyMachiningBatchDetailRepresentation representation) {
    DailyMachiningBatchDetail dailyMachiningBatchDetail = assemble(representation);
    dailyMachiningBatchDetail.setCreatedAt(LocalDateTime.now());
    return dailyMachiningBatchDetail;
  }

  public DailyMachiningBatchDetail assemble(DailyMachiningBatchDetailRepresentation representation) {
    return DailyMachiningBatchDetail.builder()
        .operationDate(representation.getOperationDate() != null ? LocalDate.parse(representation.getOperationDate()) : null)
        .startDateTime(representation.getStartDateTime() != null ? LocalDateTime.parse(representation.getStartDateTime()) : null)
        .endDateTime(representation.getEndDateTime() != null ? LocalDateTime.parse(representation.getEndDateTime()) : null)
        .completedPiecesCount(representation.getCompletedPiecesCount())
        .rejectedPiecesCount(representation.getRejectedPiecesCount())
        .reworkPiecesCount(representation.getReworkPiecesCount())
        .build();
  }

  public DailyMachiningBatchDetailRepresentation dissemble(DailyMachiningBatchDetail dailyMachiningBatchDetail){
    return DailyMachiningBatchDetailRepresentation.builder()
        .id(dailyMachiningBatchDetail.getId())
        .operationDate(String.valueOf(dailyMachiningBatchDetail.getOperationDate()))
        .startDateTime(String.valueOf(dailyMachiningBatchDetail.getStartDateTime()))
        .endDateTime(String.valueOf(dailyMachiningBatchDetail.getEndDateTime()))
        .machiningBatch(machiningBatchAssembler.dissemble(dailyMachiningBatchDetail.getMachiningBatch()))
        .completedPiecesCount(dailyMachiningBatchDetail.getCompletedPiecesCount())
        .rejectedPiecesCount(dailyMachiningBatchDetail.getRejectedPiecesCount())
        .reworkPiecesCount(dailyMachiningBatchDetail.getReworkPiecesCount())
        .build();
  }
}
