package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.Forge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForgeRepository extends CrudRepository<Forge, Long> {
  // Additional query methods if needed, for example, find by status or forging line
  Page<Forge> findByForgingLineIdInAndDeletedFalseOrderByUpdatedAtDesc(List<Long> forgingLineId, Pageable pageable);

  @Query(value = "select * FROM forge ft "
                 + "where ft.forging_line_id = :forgingLineId and ft.deleted=false and ft.forging_status != '2'"
                 + "order by ft.created_at desc LIMIT 1", nativeQuery = true)
  Optional<Forge> findAppliedForgeOnForgingLine(@Param("forgingLineId") long forgingLineId);

  @Query(value = "select * FROM forge ft "
                 + "where ft.forging_line_id = :forgingLineId and ft.deleted=false "
                 + "order by ft.created_at desc LIMIT 1", nativeQuery = true)
  Optional<Forge> findLastForgeOnForgingLine(@Param("forgingLineId") long forgingLineId);

  Optional<Forge> findByIdAndDeletedFalse(long id);
  Optional<Forge> findByForgeTraceabilityNumberAndDeletedFalse(String forgeTraceabilityNumber);
  Optional<Forge> findByIdAndAndForgingLineIdAndDeletedFalse(long id, long forgingLineId);
//  List<Forge> findByHeatIdAndDeletedFalse(long heatId);
}
