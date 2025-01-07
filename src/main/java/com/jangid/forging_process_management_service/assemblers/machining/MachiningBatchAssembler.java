package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MachiningBatchAssembler {

  @Autowired
  private DailyMachiningBatchDetailAssembler dailyMachiningBatchDetailAssembler;

  public MachiningBatch createAssemble(MachiningBatchRepresentation machiningBatchRepresentation) {
    MachiningBatch machiningBatch = assemble(machiningBatchRepresentation);
    machiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IDLE);
    machiningBatch.setCreatedAt(LocalDateTime.now());
    return machiningBatch;
  }

  public MachiningBatch assemble(MachiningBatchRepresentation machiningBatchRepresentation) {
    return MachiningBatch.builder()
        .machiningBatchNumber(machiningBatchRepresentation.getMachiningBatchNumber())
        .machiningBatchStatus(
            machiningBatchRepresentation.getMachiningBatchStatus() != null ? MachiningBatch.MachiningBatchStatus.valueOf(machiningBatchRepresentation.getMachiningBatchStatus()) : null)
        .appliedMachiningBatchPiecesCount(
            machiningBatchRepresentation.getAppliedMachiningBatchPiecesCount() != null ? Integer.valueOf(machiningBatchRepresentation.getAppliedMachiningBatchPiecesCount()) : null)
        .actualMachiningBatchPiecesCount(
            machiningBatchRepresentation.getActualMachiningBatchPiecesCount() != null ? Integer.valueOf(machiningBatchRepresentation.getActualMachiningBatchPiecesCount()) : null)
        .rejectMachiningBatchPiecesCount(
            machiningBatchRepresentation.getRejectMachiningBatchPiecesCount() != null ? Integer.valueOf(machiningBatchRepresentation.getRejectMachiningBatchPiecesCount()) : null)
        .reworkPiecesCount(machiningBatchRepresentation.getReworkPiecesCount() != null ? Integer.valueOf(machiningBatchRepresentation.getReworkPiecesCount()) : null)
        .dailyMachiningBatchDetail(machiningBatchRepresentation.getDailyMachiningBatchDetail() != null ? machiningBatchRepresentation.getDailyMachiningBatchDetail().stream()
            .map(machiningBatchPieceDetailRepresentation -> dailyMachiningBatchDetailAssembler.assemble(machiningBatchPieceDetailRepresentation)).toList() : null)
        .build();
  }

  public MachiningBatchRepresentation dissemble(MachiningBatch machiningBatch) {
    return MachiningBatchRepresentation.builder()
        .machiningBatchNumber(machiningBatch.getMachiningBatchNumber())
        .appliedMachiningBatchPiecesCount(machiningBatch.getAppliedMachiningBatchPiecesCount() != null ? String.valueOf(machiningBatch.getAppliedMachiningBatchPiecesCount()) : null)
        .actualMachiningBatchPiecesCount(machiningBatch.getActualMachiningBatchPiecesCount() != null ? String.valueOf(machiningBatch.getActualMachiningBatchPiecesCount()) : null)
        .rejectMachiningBatchPiecesCount(machiningBatch.getRejectMachiningBatchPiecesCount() != null ? String.valueOf(machiningBatch.getRejectMachiningBatchPiecesCount()) : null)
        .reworkPiecesCount(machiningBatch.getReworkPiecesCount() != null ? String.valueOf(machiningBatch.getReworkPiecesCount()) : null)
        .startAt(machiningBatch.getStartAt() != null ? String.valueOf(machiningBatch.getStartAt()) : null)
        .endAt(machiningBatch.getEndAt() != null ? String.valueOf(machiningBatch.getEndAt()) : null)
        .machiningBatchStatus(machiningBatch.getMachiningBatchStatus() != null ? String.valueOf(machiningBatch.getMachiningBatchStatus()) : null)
        .machiningBatchType(machiningBatch.getMachiningBatchType() != null ? String.valueOf(machiningBatch.getMachiningBatchType()) : null)
        .dailyMachiningBatchDetail(machiningBatch.getDailyMachiningBatchDetail() != null ? machiningBatch.getDailyMachiningBatchDetail().stream()
            .map(machiningBatchPieceDetail -> dailyMachiningBatchDetailAssembler.dissemble(machiningBatchPieceDetail)).toList() : null)
        .build();
  }
}
