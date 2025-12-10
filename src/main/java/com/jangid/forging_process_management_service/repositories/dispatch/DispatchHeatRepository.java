package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchHeat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DispatchHeatRepository extends JpaRepository<DispatchHeat, Long> {

    /**
     * Check if a heat is used in any non-deleted dispatch heat record
     * 
     * @param heatId The heat ID to check
     * @return true if heat is in use, false otherwise
     */
    boolean existsByHeatIdAndDeletedFalse(Long heatId);

    /**
     * Count how many times a heat is used in non-deleted dispatch heat records
     * 
     * @param heatId The heat ID to check
     * @return count of usage
     */
    @Query("SELECT COUNT(dh) FROM DispatchHeat dh WHERE dh.heat.id = :heatId AND dh.deleted = false")
    Long countByHeatIdAndDeletedFalse(@Param("heatId") Long heatId);
} 