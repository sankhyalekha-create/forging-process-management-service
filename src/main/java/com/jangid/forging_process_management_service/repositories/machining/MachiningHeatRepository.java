package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.MachiningHeat;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MachiningHeatRepository extends CrudRepository<MachiningHeat, Long> {

    /**
     * Check if a heat is used in any non-deleted machining heat record
     * 
     * @param heatId The heat ID to check
     * @return true if heat is in use, false otherwise
     */
    boolean existsByHeatIdAndDeletedFalse(Long heatId);

    /**
     * Count how many times a heat is used in non-deleted machining heat records
     * 
     * @param heatId The heat ID to check
     * @return count of usage
     */
    @Query("SELECT COUNT(mh) FROM MachiningHeat mh WHERE mh.heat.id = :heatId AND mh.deleted = false")
    Long countByHeatIdAndDeletedFalse(@Param("heatId") Long heatId);
}
