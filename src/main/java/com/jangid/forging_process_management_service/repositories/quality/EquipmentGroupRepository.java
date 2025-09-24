package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.EquipmentGroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentGroupRepository extends JpaRepository<EquipmentGroup, Long> {

  @Query("SELECT eg FROM EquipmentGroup eg WHERE eg.tenant.id = :tenantId AND eg.deleted = false ORDER BY eg.groupName ASC")
  List<EquipmentGroup> findAllByTenantIdAndDeletedFalse(@Param("tenantId") Long tenantId);

  @Query("SELECT eg FROM EquipmentGroup eg WHERE eg.tenant.id = :tenantId AND eg.deleted = false ORDER BY eg.groupName ASC")
  Page<EquipmentGroup> findAllByTenantIdAndDeletedFalse(@Param("tenantId") Long tenantId, Pageable pageable);

  @Query("SELECT eg FROM EquipmentGroup eg WHERE eg.id = :equipmentGroupId AND eg.tenant.id = :tenantId AND eg.deleted = false")
  Optional<EquipmentGroup> findByIdAndTenantIdAndDeletedFalse(@Param("equipmentGroupId") Long equipmentGroupId, @Param("tenantId") Long tenantId);

  @Query("SELECT COUNT(eg) > 0 FROM EquipmentGroup eg WHERE eg.groupName = :groupName AND eg.tenant.id = :tenantId AND eg.deleted = false")
  boolean existsByGroupNameAndTenantIdAndDeletedFalse(@Param("groupName") String groupName, @Param("tenantId") Long tenantId);

  @Query("SELECT COUNT(eg) > 0 FROM EquipmentGroup eg WHERE eg.groupName = :groupName AND eg.tenant.id = :tenantId AND eg.id != :equipmentGroupId AND eg.deleted = false")
  boolean existsByGroupNameAndTenantIdAndIdNotAndDeletedFalse(@Param("groupName") String groupName, @Param("tenantId") Long tenantId, @Param("equipmentGroupId") Long equipmentGroupId);
}
