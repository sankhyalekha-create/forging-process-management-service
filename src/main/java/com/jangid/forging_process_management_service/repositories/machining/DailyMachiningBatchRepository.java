package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.operator.MachineOperator;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyMachiningBatchRepository extends CrudRepository<DailyMachiningBatch, Long> {

  Optional<DailyMachiningBatch> findByIdAndDeletedFalse(long id);

  @Query("""
        SELECT CASE WHEN COUNT(dmb) > 0 THEN TRUE ELSE FALSE END
        FROM DailyMachiningBatch dmb
        WHERE dmb.machineOperator.id = :operatorId
        AND dmb.deleted = FALSE
        AND (
            (:startDateTime BETWEEN dmb.startDateTime AND dmb.endDateTime) OR
            (:endDateTime BETWEEN dmb.startDateTime AND dmb.endDateTime) OR
            (dmb.startDateTime BETWEEN :startDateTime AND :endDateTime) OR
            (dmb.endDateTime BETWEEN :startDateTime AND :endDateTime)
        )
    """)
  boolean existsOverlappingBatchForOperator(
      @Param("operatorId") Long operatorId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime
  );

  @Query("""
        SELECT dmb
        FROM DailyMachiningBatch dmb
        WHERE dmb.machineOperator.id = :operatorId
        AND dmb.deleted = FALSE
        AND (
            (:startDateTime BETWEEN dmb.startDateTime AND dmb.endDateTime) OR
            (:endDateTime BETWEEN dmb.startDateTime AND dmb.endDateTime) OR
            (dmb.startDateTime BETWEEN :startDateTime AND :endDateTime) OR
            (dmb.endDateTime BETWEEN :startDateTime AND :endDateTime)
        )
    """)
  List<DailyMachiningBatch> findOverlappingBatchesForOperator(
      @Param("operatorId") Long operatorId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime
  );


  // Need to check which function is working performs better in high volume data

  @Query(value = """
        SELECT COALESCE(SUM(
            CASE
                WHEN dmb.start_date_time < CAST(:startDateTime AS TIMESTAMP)
                     AND dmb.end_date_time > CAST(:endDateTime AS TIMESTAMP)
                    THEN EXTRACT(EPOCH FROM (CAST(:endDateTime AS TIMESTAMP) - CAST(:startDateTime AS TIMESTAMP))) / 3600.0
                WHEN dmb.start_date_time < CAST(:startDateTime AS TIMESTAMP)
                    THEN EXTRACT(EPOCH FROM (dmb.end_date_time - CAST(:startDateTime AS TIMESTAMP))) / 3600.0
                WHEN dmb.end_date_time > CAST(:endDateTime AS TIMESTAMP)
                    THEN EXTRACT(EPOCH FROM (CAST(:endDateTime AS TIMESTAMP) - dmb.start_date_time)) / 3600.0
                ELSE EXTRACT(EPOCH FROM (dmb.end_date_time - dmb.start_date_time)) / 3600.0
            END
        ), 0)
        FROM daily_machining_batch dmb
        WHERE dmb.machine_operator_id = :operatorId
        AND dmb.deleted = FALSE
        AND (
            (CAST(:startDateTime AS TIMESTAMP) BETWEEN dmb.start_date_time AND dmb.end_date_time) OR
            (CAST(:endDateTime AS TIMESTAMP) BETWEEN dmb.start_date_time AND dmb.end_date_time) OR
            (dmb.start_date_time BETWEEN CAST(:startDateTime AS TIMESTAMP) AND CAST(:endDateTime AS TIMESTAMP)) OR
            (dmb.end_date_time BETWEEN CAST(:startDateTime AS TIMESTAMP) AND CAST(:endDateTime AS TIMESTAMP))
        )
    """, nativeQuery = true)
  Double getTotalMachiningHours(
      @Param("operatorId") Long operatorId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime
  );

  List<DailyMachiningBatch> findByMachineOperatorAndStartDateTimeBetween(
      MachineOperator operator,
      LocalDateTime startDate,
      LocalDateTime endDate
  );


}
