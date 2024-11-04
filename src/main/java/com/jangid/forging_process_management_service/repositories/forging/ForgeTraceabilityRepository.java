package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForgeTraceabilityRepository extends JpaRepository<ForgeTraceability, Long> {
  // Additional query methods if needed, for example, find by status or forging line
  List<ForgeTraceability> findByForgingLineIdAndDeletedFalse(long forgingLineId);

  @Query(value = "select * FROM forge_traceability ft "
                 + "where ft.forging_line_id = :forgingLineId and ft.deleted=false and ft.forging_status != 'COMPLETED'"
                 + "order by ft.created_at desc LIMIT 1", nativeQuery = true)
  Optional<ForgeTraceability> findAppliedForgingTraceabilityOnForgingLine(@Param("forgingLineId") long forgingLineId);

  Optional<ForgeTraceability> findByIdAndDeletedFalse(long id);
}
