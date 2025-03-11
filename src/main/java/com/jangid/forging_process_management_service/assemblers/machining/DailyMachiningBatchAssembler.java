package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.assemblers.operator.MachineOperatorAssembler;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DailyMachiningBatchAssembler {

  @Autowired
  private MachineOperatorAssembler machineOperatorAssembler;

  public DailyMachiningBatch createAssemble(DailyMachiningBatchRepresentation representation) {
    DailyMachiningBatch dailyMachiningBatch = assemble(representation);
    dailyMachiningBatch.setCreatedAt(LocalDateTime.now());
    dailyMachiningBatch.setDeleted(false);
    return dailyMachiningBatch;
  }

  public DailyMachiningBatch assemble(DailyMachiningBatchRepresentation representation) {
    DailyMachiningBatch dailyMachiningBatch = new DailyMachiningBatch();
    dailyMachiningBatch.setMachineOperator(machineOperatorAssembler.assemble(representation.getMachineOperator()));

    dailyMachiningBatch.setStartDateTime(
        representation.getStartDateTime() != null ? LocalDateTime.parse(representation.getStartDateTime()) : null
    );
    dailyMachiningBatch.setEndDateTime(
        representation.getEndDateTime() != null ? LocalDateTime.parse(representation.getEndDateTime()) : null
    );
    dailyMachiningBatch.setCompletedPiecesCount(representation.getCompletedPiecesCount());
    dailyMachiningBatch.setRejectedPiecesCount(representation.getRejectedPiecesCount());
    dailyMachiningBatch.setReworkPiecesCount(representation.getReworkPiecesCount());
    dailyMachiningBatch.setDailyMachiningBatchStatus(
        representation.getDailyMachiningBatchStatus() != null
        ? DailyMachiningBatch.DailyMachiningBatchStatus.valueOf(representation.getDailyMachiningBatchStatus())
        : null
    );

    return dailyMachiningBatch;
  }

  public DailyMachiningBatchRepresentation dissemble(DailyMachiningBatch dailyMachiningBatch) {
    return DailyMachiningBatchRepresentation.builder()
        .id(dailyMachiningBatch.getId())
        .machiningBatchId(
            dailyMachiningBatch.getMachiningBatch() != null
            ? dailyMachiningBatch.getMachiningBatch().getId()
            : null
        )
        .machineOperator(dailyMachiningBatch.getMachineOperator()!=null? machineOperatorAssembler.dissembleWithoutDailyMachiningBatches(dailyMachiningBatch.getMachineOperator()):null)
        .dailyMachiningBatchStatus(
            dailyMachiningBatch.getDailyMachiningBatchStatus() != null
            ? dailyMachiningBatch.getDailyMachiningBatchStatus().name()
            : null
        )
        .startDateTime(dailyMachiningBatch.getStartDateTime() != null
                       ? dailyMachiningBatch.getStartDateTime().toString()
                       : null
        )
        .endDateTime(dailyMachiningBatch.getEndDateTime() != null
                     ? dailyMachiningBatch.getEndDateTime().toString()
                     : null
        )
        .completedPiecesCount(dailyMachiningBatch.getCompletedPiecesCount())
        .rejectedPiecesCount(dailyMachiningBatch.getRejectedPiecesCount())
        .reworkPiecesCount(dailyMachiningBatch.getReworkPiecesCount())
        .createdAt(dailyMachiningBatch.getCreatedAt() != null
                   ? dailyMachiningBatch.getCreatedAt().toString()
                   : null
        )
        .updatedAt(dailyMachiningBatch.getUpdatedAt() != null
                   ? dailyMachiningBatch.getUpdatedAt().toString()
                   : null
        )
        .deleted(dailyMachiningBatch.isDeleted())
        .build();
  }
}
