package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.MachineSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineSetRepository extends CrudRepository<MachineSet, Long> {
  // Updated methods to use direct tenant relationship instead of joining through machines
  Page<MachineSet> findByTenant_IdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
  
  List<MachineSet> findByTenant_IdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);

  boolean existsByTenant_IdAndIdAndDeletedFalse(long tenantId, long id);

  Optional<MachineSet> findByTenant_IdAndIdAndDeletedFalse(long tenantId, long id);

  /**
   * Find machine set by ID only (without tenant validation)
   * This method is used when we need to get a machine set by ID without tenant context
   */
  Optional<MachineSet> findByIdAndDeletedFalse(long machineSetId);
  
  // Updated methods for handling duplicate machine set names and reactivating deleted machine sets
  boolean existsByMachineSetNameAndTenant_IdAndDeletedFalse(String machineSetName, long tenantId);
  
  Optional<MachineSet> findByMachineSetNameAndTenant_IdAndDeletedTrue(String machineSetName, long tenantId);

  /**
   * Find machine sets that are available (not being used) during a specific time period
   * A machine set is considered available if it has no overlapping daily machining batches
   * during the specified time period
   */
  @Query("SELECT ms FROM MachineSet ms " +
         "WHERE ms.tenant.id = :tenantId " +
         "AND ms.deleted = false " +
         "AND ms.id NOT IN (" +
         "    SELECT DISTINCT dmb.machineSet.id FROM DailyMachiningBatch dmb " +
         "    WHERE dmb.deleted = false " +
         "    AND (" +
         "        (:startDateTime BETWEEN dmb.startDateTime AND dmb.endDateTime) OR " +
         "        (:endDateTime BETWEEN dmb.startDateTime AND dmb.endDateTime) OR " +
         "        (dmb.startDateTime BETWEEN :startDateTime AND :endDateTime) OR " +
         "        (dmb.endDateTime BETWEEN :startDateTime AND :endDateTime)" +
         "    )" +
         ") " +
         "ORDER BY ms.createdAt DESC")
  List<MachineSet> findAvailableMachineSetsByTenantIdAndTimeRange(
      @Param("tenantId") long tenantId,
      @Param("startDateTime") java.time.LocalDateTime startDateTime,
      @Param("endDateTime") java.time.LocalDateTime endDateTime
  );

  // Search methods for MachineSet with pagination support
  
  /**
   * Search MachineSet by machine set name (substring matching)
   * @param tenantId The tenant ID
   * @param machineSetName The machine set name to search for (substring)
   * @param pageable Pagination information
   * @return Page of MachineSet entities
   */
  @Query("""
        SELECT ms
        FROM MachineSet ms
        WHERE ms.tenant.id = :tenantId
          AND LOWER(ms.machineSetName) LIKE LOWER(CONCAT('%', :machineSetName, '%'))
          AND ms.deleted = false
        ORDER BY ms.createdAt DESC
    """)
  Page<MachineSet> findMachineSetsByMachineSetNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("machineSetName") String machineSetName, Pageable pageable);

  /**
   * Search MachineSet by machine name (substring matching)
   * @param tenantId The tenant ID
   * @param machineName The machine name to search for (substring)
   * @param pageable Pagination information
   * @return Page of MachineSet entities that contain machines with the specified name
   */
  @Query("""
        SELECT DISTINCT ms
        FROM MachineSet ms
        JOIN ms.machines m
        WHERE ms.tenant.id = :tenantId
          AND LOWER(m.machineName) LIKE LOWER(CONCAT('%', :machineName, '%'))
          AND ms.deleted = false
        ORDER BY ms.createdAt DESC
    """)
  Page<MachineSet> findMachineSetsByMachineNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("machineName") String machineName, Pageable pageable);
}
