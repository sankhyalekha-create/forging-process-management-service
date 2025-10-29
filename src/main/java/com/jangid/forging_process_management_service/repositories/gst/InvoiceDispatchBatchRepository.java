package com.jangid.forging_process_management_service.repositories.gst;

import com.jangid.forging_process_management_service.entities.gst.InvoiceDispatchBatch;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceDispatchBatchRepository extends CrudRepository<InvoiceDispatchBatch, Long> {

  Optional<InvoiceDispatchBatch> findByIdAndDeletedFalse(Long id);

  List<InvoiceDispatchBatch> findByInvoiceIdAndDeletedFalse(Long invoiceId);

  List<InvoiceDispatchBatch> findByDispatchBatchIdAndDeletedFalse(Long dispatchBatchId);

  List<InvoiceDispatchBatch> findByInvoiceIdAndDeletedFalseOrderBySequenceOrderAsc(Long invoiceId);

  boolean existsByInvoiceIdAndDispatchBatchIdAndDeletedFalse(Long invoiceId, Long dispatchBatchId);
}

