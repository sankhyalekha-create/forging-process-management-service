package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.EquipmentGroupGauge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentGroupGaugeRepository extends JpaRepository<EquipmentGroupGauge, Long> {

  @Query("SELECT COUNT(egg) > 0 FROM EquipmentGroupGauge egg WHERE egg.equipmentGroup.id = :equipmentGroupId AND egg.gauge.id = :gaugeId AND egg.tenant.id = :tenantId AND egg.deleted = false")
  boolean existsByEquipmentGroupIdAndGaugeIdAndTenantIdAndDeletedFalse(@Param("equipmentGroupId") Long equipmentGroupId, @Param("gaugeId") Long gaugeId, @Param("tenantId") Long tenantId);

  @Modifying
  @Query("UPDATE EquipmentGroupGauge egg SET egg.deleted = true, egg.deletedAt = CURRENT_TIMESTAMP WHERE egg.equipmentGroup.id = :equipmentGroupId AND egg.tenant.id = :tenantId AND egg.deleted = false")
  void softDeleteByEquipmentGroupIdAndTenantId(@Param("equipmentGroupId") Long equipmentGroupId, @Param("tenantId") Long tenantId);

  @Modifying
  @Query("UPDATE EquipmentGroupGauge egg SET egg.deleted = true, egg.deletedAt = CURRENT_TIMESTAMP WHERE egg.equipmentGroup.id = :equipmentGroupId AND egg.gauge.id = :gaugeId AND egg.tenant.id = :tenantId AND egg.deleted = false")
  void softDeleteByEquipmentGroupIdAndGaugeIdAndTenantId(@Param("equipmentGroupId") Long equipmentGroupId, @Param("gaugeId") Long gaugeId, @Param("tenantId") Long tenantId);

  @Query("SELECT egg FROM EquipmentGroupGauge egg WHERE egg.equipmentGroup.id = :equipmentGroupId AND egg.gauge.id = :gaugeId AND egg.tenant.id = :tenantId AND egg.deleted = true")
  Optional<EquipmentGroupGauge> findByEquipmentGroupIdAndGaugeIdAndTenantIdAndDeletedTrue(@Param("equipmentGroupId") Long equipmentGroupId, @Param("gaugeId") Long gaugeId, @Param("tenantId") Long tenantId);
}
