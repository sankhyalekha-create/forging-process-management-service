package com.jangid.forging_process_management_service.service.dispatch;


import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.exception.dispatch.ProcessedItemDispatchBatchNotFound;
import com.jangid.forging_process_management_service.repositories.dispatch.ProcessedItemDispatchBatchRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
public class ProcessedItemDispatchBatchService {

  @Autowired
  private ProcessedItemDispatchBatchRepository processedItemDispatchBatchRepository;

  public ProcessedItemDispatchBatch getProcessedItemDispatchBatchById(long id){
    Optional<ProcessedItemDispatchBatch> processedItemDispatchBatchOptional = processedItemDispatchBatchRepository.findByIdAndDeletedFalse(id);
    if(processedItemDispatchBatchOptional.isEmpty()){
      log.error("ProcessedItemDispatchBatch is not found for id={}", id);
      throw new ProcessedItemDispatchBatchNotFound("ProcessedItemDispatchBatch is not found for id=" + id);

    }
    return processedItemDispatchBatchOptional.get();
  }

}
