package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchPackageRepository extends JpaRepository<DispatchPackage, Long> {
    List<DispatchPackage> findByDispatchBatchIdAndDeletedFalse(Long dispatchBatchId);
    void deleteByDispatchBatchId(Long dispatchBatchId);
} 