package com.jangid.forging_process_management_service.repository.gst;

import com.jangid.forging_process_management_service.entities.gst.ChallanVendorLineItem;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallanVendorLineItemRepository extends JpaRepository<ChallanVendorLineItem, Long> {
    
    @Query("SELECT cvli FROM ChallanVendorLineItem cvli " +
           "WHERE cvli.vendorDispatchBatch.id = :vendorDispatchBatchId AND cvli.deleted = false")
    List<ChallanVendorLineItem> findByVendorDispatchBatchId(@Param("vendorDispatchBatchId") Long vendorDispatchBatchId);
    
    @Query("SELECT cvli FROM ChallanVendorLineItem cvli " +
           "WHERE cvli.deliveryChallan.id = :challanId AND cvli.deleted = false")
    List<ChallanVendorLineItem> findByDeliveryChallanId(@Param("challanId") Long challanId);
}