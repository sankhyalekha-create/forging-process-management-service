package com.jangid.forging_process_management_service.repositories.operator;

import com.jangid.forging_process_management_service.entities.operator.MachineOperator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineOperatorRepository extends CrudRepository<MachineOperator, Long> {

  Optional<MachineOperator> findByIdAndTenantId(Long id, Long tenantId);

  Optional<MachineOperator> findByIdAndDeletedFalse(long id);
  List<MachineOperator> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId);
  Page<MachineOperator> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId, Pageable pageable);

//  @Query("SELECT m FROM MachineOperator m " +
//         "WHERE m.tenant.id = :tenantId " +
//         "AND m.deleted = false " +
//         "AND m.dailyMachiningBatch IS NULL")
//  List<MachineOperator> findMachineOperatorsNotHavingAnyDailyMachiningBatchForTenant(@Param("tenantId") Long tenantId);

  @Query("SELECT m FROM MachineOperator m " +
         "WHERE m.tenant.id = :tenantId " +
         "AND m.deleted = false " +
         "AND NOT EXISTS ( " +
         "    SELECT d FROM DailyMachiningBatch d " +
         "    WHERE d.machineOperator = m " +
         "    AND d.deleted = false " +
         "    AND ((d.startDateTime BETWEEN :startDateTime AND :endDateTime) " +
         "         OR (d.endDateTime BETWEEN :startDateTime AND :endDateTime) " +
         "         OR (d.startDateTime <= :startDateTime AND d.endDateTime >= :endDateTime)) " +
         ")")
  List<MachineOperator> findNonDeletedMachineOperatorsWithoutDailyMachiningBatchForPeriod(
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime,
      @Param("tenantId") Long tenantId);


}
