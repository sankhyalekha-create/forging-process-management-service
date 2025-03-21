package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForgeHeatRepository extends CrudRepository<ForgeHeat, Long> {
    boolean existsByHeatIdAndDeletedFalse(Long heatId);
}
