package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.InspectionHeat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionHeatRepository extends JpaRepository<InspectionHeat, Long> {

    /**
     * Check if a heat is used in any non-deleted inspection heat record
     * 
     * @param heatId The heat ID to check
     * @return true if heat is in use, false otherwise
     */
    boolean existsByHeatIdAndDeletedFalse(Long heatId);

    /**
     * Count how many times a heat is used in non-deleted inspection heat records
     * 
     * @param heatId The heat ID to check
     * @return count of usage
     */
    @Query("SELECT COUNT(ih) FROM InspectionHeat ih WHERE ih.heat.id = :heatId AND ih.deleted = false")
    Long countByHeatIdAndDeletedFalse(@Param("heatId") Long heatId);
} 