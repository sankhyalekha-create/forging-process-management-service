package com.jangid.forging_process_management_service.repositories.heating;

import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentHeat;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HeatTreatmentHeatRepository extends CrudRepository<HeatTreatmentHeat, Long> {

    /**
     * Check if a heat is used in any non-deleted heat treatment heat record
     * 
     * @param heatId The heat ID to check
     * @return true if heat is in use, false otherwise
     */
    boolean existsByHeatIdAndDeletedFalse(Long heatId);

    /**
     * Count how many times a heat is used in non-deleted heat treatment heat records
     * 
     * @param heatId The heat ID to check
     * @return count of usage
     */
    @Query("SELECT COUNT(hth) FROM HeatTreatmentHeat hth WHERE hth.heat.id = :heatId AND hth.deleted = false")
    Long countByHeatIdAndDeletedFalse(@Param("heatId") Long heatId);
}
