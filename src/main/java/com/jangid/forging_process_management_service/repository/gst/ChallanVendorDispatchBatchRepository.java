package com.jangid.forging_process_management_service.repository.gst;

import com.jangid.forging_process_management_service.entities.gst.ChallanVendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallanVendorDispatchBatchRepository extends JpaRepository<ChallanVendorDispatchBatch, Long> {

    Optional<ChallanVendorDispatchBatch> findByDeliveryChallanAndVendorDispatchBatch(DeliveryChallan challan, VendorDispatchBatch vendorDispatchBatch);

    @Query("SELECT cvdb FROM ChallanVendorDispatchBatch cvdb " +
           "WHERE cvdb.vendorDispatchBatch.id = :vendorDispatchBatchId AND cvdb.deleted = false")
    List<ChallanVendorDispatchBatch> findByVendorDispatchBatchId(@Param("vendorDispatchBatchId") Long vendorDispatchBatchId);

    @Query("SELECT cvdb FROM ChallanVendorDispatchBatch cvdb " +
           "WHERE cvdb.deliveryChallan.id = :challanId AND cvdb.deleted = false")
    List<ChallanVendorDispatchBatch> findByDeliveryChallanId(@Param("challanId") Long challanId);
}