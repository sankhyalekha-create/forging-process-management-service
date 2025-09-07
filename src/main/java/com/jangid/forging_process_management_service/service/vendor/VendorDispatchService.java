package com.jangid.forging_process_management_service.service.vendor;

import com.jangid.forging_process_management_service.assemblers.vendor.VendorDispatchBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.vendor.ProcessedItemVendorDispatchBatchAssembler;
import com.jangid.forging_process_management_service.dto.workflow.WorkflowOperationContext;
import com.jangid.forging_process_management_service.entities.ConsumptionType;
import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchHeat;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entities.vendor.VendorProcessType;
import com.jangid.forging_process_management_service.entities.vendor.VendorReceiveBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.ProcessedItemVendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.repositories.TenantRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorEntityRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorRepository;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class VendorDispatchService {

    private final VendorDispatchBatchRepository vendorDispatchBatchRepository;
    private final VendorRepository vendorRepository;
    private final VendorEntityRepository vendorEntityRepository;
    private final TenantRepository tenantRepository;
    private final VendorDispatchBatchAssembler vendorDispatchBatchAssembler;
    private final ProcessedItemVendorDispatchBatchAssembler processedItemVendorDispatchBatchAssembler;
    private final ItemWorkflowService itemWorkflowService;
    private final RawMaterialHeatService rawMaterialHeatService;
    private final VendorInventoryService vendorInventoryService;
    @Lazy
    private final VendorReceiveService vendorReceiveService;
    private final ProcessedItemVendorDispatchBatchService processedItemVendorDispatchBatchService;

    @Autowired
    public VendorDispatchService(
        VendorDispatchBatchRepository vendorDispatchBatchRepository,
        VendorRepository vendorRepository,
        VendorEntityRepository vendorEntityRepository,
        TenantRepository tenantRepository,
        VendorDispatchBatchAssembler vendorDispatchBatchAssembler,
        ProcessedItemVendorDispatchBatchAssembler processedItemVendorDispatchBatchAssembler,
        ItemWorkflowService itemWorkflowService,
        RawMaterialHeatService rawMaterialHeatService,
        VendorInventoryService vendorInventoryService,
        @Lazy VendorReceiveService vendorReceiveService, ProcessedItemVendorDispatchBatchService processedItemVendorDispatchBatchService) {
        this.vendorDispatchBatchRepository = vendorDispatchBatchRepository;
        this.vendorRepository = vendorRepository;
        this.vendorEntityRepository = vendorEntityRepository;
        this.tenantRepository = tenantRepository;
        this.vendorDispatchBatchAssembler = vendorDispatchBatchAssembler;
        this.processedItemVendorDispatchBatchAssembler = processedItemVendorDispatchBatchAssembler;
        this.itemWorkflowService = itemWorkflowService;
        this.rawMaterialHeatService = rawMaterialHeatService;
        this.vendorInventoryService = vendorInventoryService;
        this.vendorReceiveService = vendorReceiveService;
        this.processedItemVendorDispatchBatchService = processedItemVendorDispatchBatchService;
    }

    @Transactional(rollbackFor = Exception.class)
    public VendorDispatchBatchRepresentation createVendorDispatchBatch(
            VendorDispatchBatchRepresentation representation, Long tenantId) {
        
        log.info("Starting vendor dispatch batch creation transaction for tenant: {}, batch: {}", 
                tenantId, representation.getVendorDispatchBatchNumber());

        VendorDispatchBatch savedBatch = null;
        try {
            // Phase 1: Validate inputs and entities
            Tenant tenant = validateTenantAndEntities(representation, tenantId);
            
            // Phase 2: Setup vendor dispatch batch
            VendorDispatchBatch batch = setupVendorDispatchBatch(representation, tenant);
            
            // Phase 3: Process the vendor dispatch batch item with workflow integration
            processVendorDispatchBatchItem(representation, batch);
            
            // Phase 4: Save the batch - this generates the required ID for workflow integration
            savedBatch = vendorDispatchBatchRepository.save(batch);
            log.info("Successfully persisted vendor dispatch batch with ID: {}", savedBatch.getId());

            // Phase 5: Handle workflow integration - if this fails, entire transaction will rollback
            if (savedBatch.getProcessedItem() != null) {
                log.info("Starting workflow integration for vendor dispatch batch ID: {}", savedBatch.getId());
                handleWorkflowIntegration(representation, savedBatch.getProcessedItem());
                log.info("Successfully completed workflow integration for vendor dispatch batch ID: {}", savedBatch.getId());
            }

            log.info("Successfully completed vendor dispatch batch creation transaction for ID: {}", savedBatch.getId());
            return vendorDispatchBatchAssembler.dissemble(savedBatch);
            
        } catch (Exception e) {
            log.error("Vendor dispatch batch creation transaction failed for tenant: {}, batch: {}. " +
                      "All changes will be rolled back. Error: {}", 
                      tenantId, representation.getVendorDispatchBatchNumber(), e.getMessage());
            
            if (savedBatch != null) {
                log.error("Vendor dispatch batch with ID {} was persisted but workflow integration failed. " +
                          "Transaction rollback will restore database consistency.", savedBatch.getId());
            }
            
            // Re-throw to ensure transaction rollback
            throw e;
        }
    }

    /**
     * Phase 1: Validate tenant and all required entities
     */
    private Tenant validateTenantAndEntities(VendorDispatchBatchRepresentation representation, Long tenantId) {
        // Validate tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        // Check if batch number already exists
        if (vendorDispatchBatchRepository.existsByVendorDispatchBatchNumberAndTenantIdAndDeletedFalse(
                representation.getVendorDispatchBatchNumber(), tenantId)) {
            throw new ValidationException("Vendor dispatch batch number already exists: " + 
                    representation.getVendorDispatchBatchNumber());
        }

        // Validate vendor
        if (representation.getVendor() == null) {
            throw new ValidationException("Vendor ID is required");
        }

        // Validate billing and shipping entities
        if (representation.getBillingEntityId() == null || representation.getShippingEntityId() == null) {
            throw new ValidationException("Billing and shipping entity IDs are required");
        }

        // Validate processes
        if (representation.getProcesses() == null || representation.getProcesses().isEmpty()) {
            throw new ValidationException("At least one process must be specified");
        }

        return tenant;
    }

    /**
     * Phase 2: Setup vendor dispatch batch with basic properties
     */
    private VendorDispatchBatch setupVendorDispatchBatch(VendorDispatchBatchRepresentation representation, Tenant tenant) {
        // Validate and get vendor
        Vendor vendor = vendorRepository.findById(representation.getVendor().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + representation.getVendor()));

        // Validate billing entity
        VendorEntity billingEntity = vendorEntityRepository.findById(representation.getBillingEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Billing entity not found with id: " + representation.getBillingEntityId()));

        // Validate shipping entity
        VendorEntity shippingEntity = vendorEntityRepository.findById(representation.getShippingEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping entity not found with id: " + representation.getShippingEntityId()));

        // Create dispatch batch
        VendorDispatchBatch batch = VendorDispatchBatch.builder()
                .vendorDispatchBatchNumber(representation.getVendorDispatchBatchNumber())
                .originalVendorDispatchBatchNumber(representation.getVendorDispatchBatchNumber())
                .vendorDispatchBatchStatus(VendorDispatchBatch.VendorDispatchBatchStatus.DISPATCHED)
                .remarks(representation.getRemarks())
                .packagingType(representation.getPackagingType() != null ?
                               PackagingType.valueOf(representation.getPackagingType()) : null)
                .packagingQuantity(representation.getPackagingQuantity())
                .perPackagingQuantity(representation.getPerPackagingQuantity())
                .useUniformPackaging(representation.getUseUniformPackaging())
                // Entity references
                .tenant(tenant)
                .vendor(vendor)
                .billingEntity(billingEntity)
                .shippingEntity(shippingEntity)
                .processes(representation.getProcesses())
                .dispatchedAt(ConvertorUtils.convertStringToLocalDateTime(representation.getDispatchedAt()))
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        return batch;
    }

    /**
     * Phase 3: Process the vendor dispatch batch item with workflow integration
     */
    private void processVendorDispatchBatchItem(VendorDispatchBatchRepresentation representation, 
                                               VendorDispatchBatch batch) {
        // Process the processed item vendor dispatch batch from the representation
        if (representation.getProcessedItem() != null) {
            ProcessedItemVendorDispatchBatch processedItem = 
                processedItemVendorDispatchBatchAssembler.createAssemble(representation.getProcessedItem(), representation.getItemWeightType());

            // Finalize processed item setup and add to the batch
            finalizeProcessedItemSetup(processedItem, batch);
            
            // Set the processed item to the batch
            batch.setProcessedItem(processedItem);
        }
    }

    /**
     * Finalize processed item setup
     */
    private void finalizeProcessedItemSetup(ProcessedItemVendorDispatchBatch processedItem, 
                                           VendorDispatchBatch batch) {
        processedItem.markAsDispatched();
        processedItem.setVendorDispatchBatch(batch);
        
        // Set available pieces count equal to the dispatched pieces count initially
        if (processedItem.getIsInPieces()) {
            processedItem.setDispatchedPiecesCount(processedItem.getDispatchedPiecesCount());
        } else {
            processedItem.setDispatchedQuantity(processedItem.getDispatchedQuantity());
        }
    }

    /**
     * Handle workflow integration including pieces consumption and workflow step updates
     */
    private void handleWorkflowIntegration(VendorDispatchBatchRepresentation representation, 
                                          ProcessedItemVendorDispatchBatch processedItem) {
        try {
            ItemWorkflow workflow = getOrValidateWorkflow(processedItem);

            // Determine operation position and get workflow step
            WorkflowOperationContext operationContext = createOperationContext(processedItem, workflow);

            // Start workflow step operation
            itemWorkflowService.startItemWorkflowStepOperation(operationContext.getTargetWorkflowStep());

            // Handle inventory/pieces consumption based on operation position
            handleInventoryConsumption(representation, processedItem, workflow, operationContext.isFirstOperation());

            // Update workflow step with batch outcome data
            updateWorkflowStepWithBatchOutcome(processedItem, operationContext.isFirstOperation(), operationContext.getTargetWorkflowStep());

            // Update related entity IDs
            updateRelatedEntityIds(operationContext.getTargetWorkflowStep(), processedItem.getId());
        } catch (Exception e) {
            log.error("Failed to integrate vendor dispatch batch with workflow for item {}: {}",
                      processedItem.getItem().getId(), e.getMessage());
            // Re-throw to fail the operation since workflow integration is critical
            throw new RuntimeException("Failed to integrate with workflow system: " + e.getMessage(), e);
        }
    }

    private ItemWorkflow getOrValidateWorkflow(ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch) {
        Long itemWorkflowId = processedItemVendorDispatchBatch.getItemWorkflowId();
        if (itemWorkflowId == null) {
            throw new IllegalStateException("ItemWorkflowId is required for Vendor Dispatch batch integration");
        }

        ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);

        // Ensure workflow ID is set (defensive programming)
        if (processedItemVendorDispatchBatch.getItemWorkflowId() == null) {
            processedItemVendorDispatchBatch.setItemWorkflowId(workflow.getId());
            processedItemVendorDispatchBatchService.save(processedItemVendorDispatchBatch);
        }

        return workflow;
    }

    private WorkflowOperationContext createOperationContext(ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch, ItemWorkflow workflow) {
        Long previousOperationProcessedItemId = processedItemVendorDispatchBatch.getPreviousOperationProcessedItemId();

        // Determine if this is the first operation
        boolean isFirstOperation = isFirstWorkflowOperation(previousOperationProcessedItemId, workflow);

        // Find the appropriate workflow step
        ItemWorkflowStep targetVendorStep = findTargetVendorStep(workflow, previousOperationProcessedItemId, isFirstOperation);

        return new WorkflowOperationContext(isFirstOperation, targetVendorStep, previousOperationProcessedItemId);
    }

    private boolean isFirstWorkflowOperation(Long previousOperationProcessedItemId, ItemWorkflow workflow) {
        return previousOperationProcessedItemId == null ||
               WorkflowStep.OperationType.VENDOR.equals(workflow.getWorkflowTemplate().getFirstStep().getOperationType());
    }

    private ItemWorkflowStep findTargetVendorStep(ItemWorkflow workflow, Long previousOperationProcessedItemId, boolean isFirstOperation) {
        if (isFirstOperation) {
            return workflow.getFirstRootStep();
        } else {
            return itemWorkflowService.findItemWorkflowStepByParentEntityId(
                workflow.getId(),
                previousOperationProcessedItemId,
                WorkflowStep.OperationType.VENDOR);
        }
    }

    private void handleInventoryConsumption(VendorDispatchBatchRepresentation representation,
                                            ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch,
                                            ItemWorkflow workflow,
                                            boolean isFirstOperation) {
        if (isFirstOperation) {
            log.info("Vendor is the first operation in workflow - consuming inventory from heat");
            handleHeatConsumptionForFirstOperation(representation, processedItemVendorDispatchBatch, workflow);
        } else {
            handlePiecesConsumptionFromPreviousOperation(processedItemVendorDispatchBatch, workflow);
        }
    }

    /**
     * Handles pieces consumption from previous operation (if applicable)
     * Uses optimized single-call method to improve performance
     */
    private void handlePiecesConsumptionFromPreviousOperation(ProcessedItemVendorDispatchBatch processedItem, 
                                                             ItemWorkflow workflow) {
        // Use the optimized method that combines find + validate + consume in a single efficient call
        try {
            ItemWorkflowStep parentOperationStep = itemWorkflowService.validateAndConsumePiecesFromParentOperation(
                workflow.getId(),
                WorkflowStep.OperationType.VENDOR,
                processedItem.getPreviousOperationProcessedItemId(),
                processedItem.getDispatchedPiecesCount()
            );

            log.info("Efficiently consumed {} pieces from {} operation {} for vendor in workflow {}",
                     processedItem.getDispatchedPiecesCount(),
                     parentOperationStep.getOperationType(),
                     processedItem.getPreviousOperationProcessedItemId(),
                     workflow.getId());

        } catch (IllegalArgumentException e) {
            // Re-throw with context for vendor dispatch batch
            log.error("Failed to consume pieces for vendor dispatch batch: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Handles heat consumption from inventory when vendor is the first operation
     */
    private void handleHeatConsumptionForFirstOperation(VendorDispatchBatchRepresentation representation,
                                                       ProcessedItemVendorDispatchBatch processedItem,
                                                       ItemWorkflow workflow) {
        // Get the corresponding processed item representation to access heat data
        ProcessedItemVendorDispatchBatchRepresentation processedItemRepresentation = representation.getProcessedItem();
        
        if (processedItemRepresentation == null || 
            processedItemRepresentation.getVendorDispatchHeats() == null || 
            processedItemRepresentation.getVendorDispatchHeats().isEmpty()) {
            log.warn("No heat consumption data provided for first operation vendor dispatch batch processed item {}. This may result in inventory inconsistency.",
                     processedItem.getId());
            return;
        }

        // Validate that heat consumption matches the required pieces
        int totalPiecesFromHeats = processedItemRepresentation.getVendorDispatchHeats().stream()
            .mapToInt(heat -> {
                if ("PIECES".equals(heat.getConsumptionType())) {
                    return heat.getPiecesUsed() != null ? heat.getPiecesUsed() : 0;
                }
                return 0;
            })
            .sum();

        BigDecimal totalQuantityFromHeats = processedItemRepresentation.getVendorDispatchHeats().stream()
            .filter(heat -> "QUANTITY".equals(heat.getConsumptionType()))
            .map(heat -> heat.getQuantityUsed() != null ? BigDecimal.valueOf(heat.getQuantityUsed()) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Determine if FORGING is the first process
        boolean isFirstProcessForging = !representation.getProcesses().isEmpty() && 
                                       representation.getProcesses().get(0) == VendorProcessType.FORGING;

        if (isFirstProcessForging) {
            // For FORGING as first process, we expect QUANTITY consumption
            if (totalQuantityFromHeats.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("No quantity consumption found for FORGING as first process in vendor dispatch batch");
            }
            log.info("Processing QUANTITY-based heat consumption for FORGING as first process: {} KG", totalQuantityFromHeats);
        } else {
            // For non-FORGING first process, we expect PIECES consumption
            if (totalPiecesFromHeats != processedItem.getDispatchedPiecesCount()) {
                throw new IllegalArgumentException("Total pieces from heats (" + totalPiecesFromHeats + 
                                                  ") does not match dispatched pieces count (" + 
                                                  processedItem.getDispatchedPiecesCount() + 
                                                  ") for processed item " + processedItem.getId());
            }
            log.info("Processing PIECES-based heat consumption for non-FORGING first process: {} pieces", totalPiecesFromHeats);
        }

        // Get the vendor dispatch batch create time for validation
        LocalDateTime dispatchedAtLocalDateTime = representation.getDispatchedAt() != null ?
                                                  ConvertorUtils.convertStringToLocalDateTime(representation.getDispatchedAt()) : LocalDateTime.now();

        // Create and setup vendor dispatch heats
        List<VendorDispatchHeat> vendorDispatchHeats = new ArrayList<>();
        
        // Consume material from existing VendorInventory for VendorDispatchBatch
        processedItemRepresentation.getVendorDispatchHeats().forEach(heatRepresentation -> {
            try {
                // Get the heat entity for validation
                Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatRepresentation.getHeat().getId());
                
                // Validate timing - heat should be received before the vendor dispatch create time
                LocalDateTime rawMaterialReceivingDate = heat.getRawMaterialProduct().getRawMaterial().getRawMaterialReceivingDate();
                if (rawMaterialReceivingDate != null && rawMaterialReceivingDate.compareTo(dispatchedAtLocalDateTime) > 0) {
                    log.error("The provided dispatched at time={} is before raw material receiving date={} for heat={} !",
                              dispatchedAtLocalDateTime, rawMaterialReceivingDate, heat.getHeatNumber());
                    throw new RuntimeException("The provided dispatched at time=" + dispatchedAtLocalDateTime +
                                               " is before raw material receiving date=" + rawMaterialReceivingDate +
                                               " for heat=" + heat.getHeatNumber() + " !");
                }
                
                // Consume material from vendor inventory instead of transferring from Heat
                Vendor vendor = processedItem.getVendorDispatchBatch().getVendor();
                
                if ("QUANTITY".equals(heatRepresentation.getConsumptionType())) {
                    // Consume quantity from vendor inventory
                    boolean success = vendorInventoryService.consumeFromVendorInventory(
                        vendor.getId(), heat.getHeatNumber(), heatRepresentation.getQuantityUsed(), null);
                    
                    if (!success) {
                        throw new IllegalArgumentException("Failed to consume " + heatRepresentation.getQuantityUsed() + 
                                                         " KG from vendor inventory for heat " + heat.getHeatNumber());
                    }
                    
                    log.info("Consumed {} KG from vendor {} inventory for heat {} in vendor dispatch batch processed item {}", 
                             heatRepresentation.getQuantityUsed(), vendor.getId(), heat.getHeatNumber(), processedItem.getId());
                             
                } else if ("PIECES".equals(heatRepresentation.getConsumptionType())) {
                    // Consume pieces from vendor inventory
                    boolean success = vendorInventoryService.consumeFromVendorInventory(
                        vendor.getId(), heat.getHeatNumber(), null, heatRepresentation.getPiecesUsed());
                    
                    if (!success) {
                        throw new IllegalArgumentException("Failed to consume " + heatRepresentation.getPiecesUsed() + 
                                                         " pieces from vendor inventory for heat " + heat.getHeatNumber());
                    }
                    
                    log.info("Consumed {} pieces from vendor {} inventory for heat {} in vendor dispatch batch processed item {}", 
                             heatRepresentation.getPiecesUsed(), vendor.getId(), heat.getHeatNumber(), processedItem.getId());
                } else {
                    throw new IllegalArgumentException("Invalid consumption type: " + heatRepresentation.getConsumptionType());
                }
                
                // Create VendorDispatchHeat entity to track the consumption
                VendorDispatchHeat vendorDispatchHeat = VendorDispatchHeat.builder()
                    .processedItemVendorDispatchBatch(processedItem)
                    .heat(heat)
                    .consumptionType(ConsumptionType.valueOf(heatRepresentation.getConsumptionType()))
                    .quantityUsed(heatRepresentation.getQuantityUsed())
                    .piecesUsed(heatRepresentation.getPiecesUsed())
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .build();
                
                vendorDispatchHeats.add(vendorDispatchHeat);
                
                log.info("Successfully consumed {} {} from vendor inventory for vendor dispatch batch processed item {} in workflow {}", 
                         "QUANTITY".equals(heatRepresentation.getConsumptionType()) ? 
                             heatRepresentation.getQuantityUsed() + " KG" : heatRepresentation.getPiecesUsed() + " pieces",
                         heatRepresentation.getConsumptionType().toLowerCase(),
                         processedItem.getId(),
                         workflow.getId());
                
            } catch (Exception e) {
                log.error("Failed to consume from vendor inventory for heat {} in vendor dispatch processed item {}: {}", 
                          heatRepresentation.getHeat().getId(), processedItem.getId(), e.getMessage());
                throw new RuntimeException("Failed to consume from vendor inventory: " + e.getMessage(), e);
            }
        });

        // Clear existing collection and add all new vendor dispatch heats
        // This prevents the Hibernate "cascade all-delete-orphan" error by modifying 
        // the existing collection instead of replacing it
        processedItem.getVendorDispatchHeats().clear();
        processedItem.getVendorDispatchHeats().addAll(vendorDispatchHeats);

        log.info("Successfully consumed from vendor inventory using {} heats for vendor dispatch batch processed item {} in workflow {}", 
                 processedItemRepresentation.getVendorDispatchHeats().size(), processedItem.getId(), workflow.getId());
    }


    private void updateWorkflowStepWithBatchOutcome(ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch, boolean isFirstOperation, ItemWorkflowStep operationStep) {
        // Create vendorBatchOutcome object with available data from processedItemVendorDispatchBatch
        OperationOutcomeData.BatchOutcome vendorBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
            .id(processedItemVendorDispatchBatch.getId())
            .initialPiecesCount(0)
            .piecesAvailableForNext(0)
            .createdAt(processedItemVendorDispatchBatch.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .deletedAt(processedItemVendorDispatchBatch.getDeletedAt())
            .deleted(processedItemVendorDispatchBatch.getDeleted())
            .build();

        List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = new ArrayList<>();

        if (!isFirstOperation) {
            accumulatedBatchData = itemWorkflowService.getAccumulatedBatchOutcomeData(operationStep);
        }
        accumulatedBatchData.add(vendorBatchOutcome);
        itemWorkflowService.updateWorkflowStepForOperation(operationStep, OperationOutcomeData.forVendorOperation(accumulatedBatchData, LocalDateTime.now()));
    }

    private void updateRelatedEntityIds(ItemWorkflowStep targetVendorStep, Long processedItemId) {
        if (targetVendorStep != null) {
            itemWorkflowService.updateRelatedEntityIdsForSpecificStep(targetVendorStep, processedItemId);
        } else {
            log.warn("Could not find target VENDOR ItemWorkflowStep for vendor dispatch batch {}", processedItemId);
        }
    }

    @Transactional(readOnly = true)
    public VendorDispatchBatchRepresentation getVendorDispatchBatch(Long batchId, Long tenantId) {
        VendorDispatchBatch batch = vendorDispatchBatchRepository.findByIdAndTenantIdAndDeletedFalse(batchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor dispatch batch not found with id: " + batchId));
        
        return vendorDispatchBatchAssembler.dissemble(batch, true);
    }

    @Transactional(readOnly = true)
    public List<VendorDispatchBatchRepresentation> getAllVendorDispatchBatchesWithoutPagination(Long tenantId) {
        List<VendorDispatchBatch> batches = vendorDispatchBatchRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
        return batches.stream()
                .map(batch -> vendorDispatchBatchAssembler.dissemble(batch, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<VendorDispatchBatchRepresentation> getAllVendorDispatchBatches(Long tenantId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorDispatchBatch> batches = vendorDispatchBatchRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);
        return batches.map(batch -> vendorDispatchBatchAssembler.dissemble(batch, true));
    }

    @Transactional(readOnly = true)
    public List<VendorDispatchBatchRepresentation> getVendorDispatchBatchesByVendorWithoutPagination(Long vendorId, Long tenantId) {
        List<VendorDispatchBatch> batches = vendorDispatchBatchRepository
                .findByVendorIdAndTenantIdAndDeletedFalse(vendorId, tenantId);
        return batches.stream()
                .map(batch -> vendorDispatchBatchAssembler.dissemble(batch, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<VendorDispatchBatchRepresentation> getVendorDispatchBatchesByVendor(Long vendorId, Long tenantId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorDispatchBatch> batches = vendorDispatchBatchRepository
                .findByVendorIdAndTenantIdAndDeletedFalse(vendorId, tenantId, pageable);
        return batches.map(batch -> vendorDispatchBatchAssembler.dissemble(batch, true));
    }

    @Transactional(readOnly = true)
    public List<VendorDispatchBatchRepresentation> getVendorDispatchBatchesByProcessedItemVendorDispatchBatchIds(List<Long> processedItemVendorDispatchBatchIds, Long tenantId) {
        if (processedItemVendorDispatchBatchIds == null || processedItemVendorDispatchBatchIds.isEmpty()) {
            log.info("No processed item vendor dispatch batch IDs provided, returning empty list");
            return Collections.emptyList();
        }

        log.info("Getting vendor dispatch batches for {} processed item vendor dispatch batch IDs for tenant {}", processedItemVendorDispatchBatchIds.size(), tenantId);
        
        List<VendorDispatchBatch> vendorDispatchBatches = vendorDispatchBatchRepository.findByProcessedItemIdInAndDeletedFalse(processedItemVendorDispatchBatchIds);
        
        List<VendorDispatchBatchRepresentation> validVendorDispatchBatches = new ArrayList<>();
        List<Long> invalidProcessedItemVendorDispatchBatchIds = new ArrayList<>();
        
        for (Long processedItemVendorDispatchBatchId : processedItemVendorDispatchBatchIds) {
            Optional<VendorDispatchBatch> vendorDispatchBatchOpt = vendorDispatchBatches.stream()
                    .filter(vdb -> vdb.getProcessedItem() != null && 
                                  vdb.getProcessedItem().getId().equals(processedItemVendorDispatchBatchId))
                    .findFirst();
                    
            if (vendorDispatchBatchOpt.isPresent()) {
                VendorDispatchBatch vendorDispatchBatch = vendorDispatchBatchOpt.get();
                if (Long.valueOf(vendorDispatchBatch.getTenant().getId()).equals(tenantId)) {
                    validVendorDispatchBatches.add(vendorDispatchBatchAssembler.dissemble(vendorDispatchBatch, true));
                } else {
                    log.warn("VendorDispatchBatch for processedItemVendorDispatchBatchId={} does not belong to tenant={}", processedItemVendorDispatchBatchId, tenantId);
                    invalidProcessedItemVendorDispatchBatchIds.add(processedItemVendorDispatchBatchId);
                }
            } else {
                log.warn("No vendor dispatch batch found for processedItemVendorDispatchBatchId={}", processedItemVendorDispatchBatchId);
                invalidProcessedItemVendorDispatchBatchIds.add(processedItemVendorDispatchBatchId);
            }
        }
        
        if (!invalidProcessedItemVendorDispatchBatchIds.isEmpty()) {
            log.warn("The following processed item vendor dispatch batch IDs did not have valid vendor dispatch batches for tenant {}: {}", 
                     tenantId, invalidProcessedItemVendorDispatchBatchIds);
        }
        
        log.info("Found {} valid vendor dispatch batches out of {} requested processed item vendor dispatch batch IDs", validVendorDispatchBatches.size(), processedItemVendorDispatchBatchIds.size());
        return validVendorDispatchBatches;
    }

    public VendorDispatchBatchRepresentation getVendorDispatchBatchByProcessedItemVendorDispatchBatchId(Long processedItemVendorDispatchBatchId) {
        if (processedItemVendorDispatchBatchId == null ) {
            log.info("No processed item inspection batch ID provided, returning null");
            return null;
        }


        Optional<VendorDispatchBatch> vendorDispatchBatchOptional = vendorDispatchBatchRepository.findByProcessedItemIdAndDeletedFalse(processedItemVendorDispatchBatchId);

        if (vendorDispatchBatchOptional.isPresent()) {
            VendorDispatchBatch vendorDispatchBatch = vendorDispatchBatchOptional.get();
            return vendorDispatchBatchAssembler.dissemble(vendorDispatchBatch);
        } else {
            log.error("No VendorDispatchBatch found for processedItemVendorDispatchBatchId={}", processedItemVendorDispatchBatchId);
            throw new RuntimeException("No inspection batch found for processedItemVendorDispatchBatchId=" + processedItemVendorDispatchBatchId);
        }

    }


    @Transactional(readOnly = true)
    public Page<VendorDispatchBatchRepresentation> searchVendorDispatchBatches(Long tenantId, String searchType, String searchTerm, int page, int size) {
        log.info("Searching vendor dispatch batches for tenant {} with search type {} and term '{}'", tenantId, searchType, searchTerm);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorDispatchBatch> batches;
        
        switch (searchType) {
            case "VENDOR_DISPATCH_BATCH_NUMBER":
                batches = vendorDispatchBatchRepository.findByVendorDispatchBatchNumberContainingIgnoreCaseAndTenantIdAndDeletedFalse(
                        searchTerm, tenantId, pageable);
                break;
            case "ITEM_NAME":
                batches = vendorDispatchBatchRepository.findByProcessedItemItemItemNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(
                        searchTerm, tenantId, pageable);
                break;
            case "ITEM_WORKFLOW_NAME":
                batches = vendorDispatchBatchRepository.findByProcessedItemWorkflowIdentifierContainingIgnoreCaseAndTenantIdAndDeletedFalse(
                        searchTerm, tenantId, pageable);
                break;
            case "VENDOR_RECEIVE_BATCH_NUMBER":
                batches = vendorDispatchBatchRepository.findByVendorReceiveBatchesVendorReceiveBatchNumberContainingIgnoreCaseAndTenantIdAndDeletedFalse(
                        searchTerm, tenantId, pageable);
                break;
            default:
                throw new IllegalArgumentException("Invalid search type: " + searchType);
        }
        
        log.info("Found {} vendor dispatch batches matching search criteria", batches.getTotalElements());
        return batches.map(batch -> vendorDispatchBatchAssembler.dissemble(batch, true));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteVendorDispatchBatch(Long batchId, Long tenantId) {
        log.info("Starting vendor dispatch batch deletion transaction for tenant: {}, batch: {}", 
                 tenantId, batchId);
        
        try {
            // Phase 1: Validate all deletion preconditions
            VendorDispatchBatch batch = validateVendorDispatchBatchDeletionPreconditions(tenantId, batchId);
            
            // Phase 2: Delete all associated vendor receive batches first
            deleteAssociatedVendorReceiveBatches(batch, tenantId);
            
            // Phase 3: Process inventory reversal for processed item - CRITICAL: Workflow and vendor inventory operations
            ProcessedItemVendorDispatchBatch processedItem = batch.getProcessedItem();
            processInventoryReversalForProcessedItem(processedItem, batchId);
            
            // Phase 4: Soft delete processed item and associated records
            softDeleteProcessedItemAndAssociatedRecords(batch, processedItem);
            
            // Phase 5: Finalize vendor dispatch batch deletion
            finalizeVendorDispatchBatchDeletion(batch, batchId);
            log.info("Successfully persisted vendor dispatch batch deletion with ID: {}", batchId);
            
            log.info("Successfully completed vendor dispatch batch deletion transaction for ID: {}", batchId);
            
        } catch (Exception e) {
            log.error("Vendor dispatch batch deletion transaction failed for tenant: {}, batch: {}. " +
                      "All changes will be rolled back. Error: {}", 
                      tenantId, batchId, e.getMessage());
            
            log.error("Vendor dispatch batch deletion failed - workflow updates, vendor inventory reversals, and entity deletions will be rolled back.");
            
            // Re-throw to ensure transaction rollback
            throw e;
        }
    }

    /**
     * Phase 2: Delete all associated vendor receive batches in reverse chronological order
     */
    private void deleteAssociatedVendorReceiveBatches(VendorDispatchBatch batch, Long tenantId) {
        log.info("Deleting associated vendor receive batches for dispatch batch: {}", batch.getId());
        
        if (batch.getVendorReceiveBatches() == null || batch.getVendorReceiveBatches().isEmpty()) {
            log.info("No vendor receive batches found for dispatch batch: {}", batch.getId());
            return;
        }
        
        // Get all non-deleted vendor receive batches and sort them in reverse chronological order (newest first)
        List<VendorReceiveBatch> receiveBatchesToDelete = batch.getVendorReceiveBatches().stream()
                .filter(receiveBatch -> !receiveBatch.isDeleted())
                .sorted((batch1, batch2) -> batch2.getCreatedAt().compareTo(batch1.getCreatedAt())) // Sort newest first
                .collect(Collectors.toList());
        
        if (receiveBatchesToDelete.isEmpty()) {
            log.info("No active vendor receive batches found for dispatch batch: {}", batch.getId());
            return;
        }
        
        log.info("Found {} vendor receive batches to delete for dispatch batch: {}", 
                 receiveBatchesToDelete.size(), batch.getId());
        
        // Delete each vendor receive batch in reverse chronological order using the existing service method
        for (VendorReceiveBatch receiveBatch : receiveBatchesToDelete) {
            try {
                log.info("Deleting vendor receive batch: {} (batch number: {}) for dispatch batch: {}", 
                         receiveBatch.getId(), receiveBatch.getVendorReceiveBatchNumber(), batch.getId());
                
                vendorReceiveService.deleteVendorReceiveBatch(receiveBatch.getId(), tenantId);
                
                log.info("Successfully deleted vendor receive batch: {} for dispatch batch: {}", 
                         receiveBatch.getId(), batch.getId());
                         
            } catch (Exception e) {
                String errorMessage = String.format(
                    "Failed to delete vendor receive batch %d (batch number: %s) for dispatch batch %d: %s", 
                    receiveBatch.getId(), receiveBatch.getVendorReceiveBatchNumber(), batch.getId(), e.getMessage());
                    
                log.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }
        
        log.info("Successfully deleted all {} vendor receive batches for dispatch batch: {}", 
                 receiveBatchesToDelete.size(), batch.getId());
    }

    /**
     * Phase 3: Validate all deletion preconditions
     */
    private VendorDispatchBatch validateVendorDispatchBatchDeletionPreconditions(Long tenantId, Long batchId) {
        // 1. Validate tenant exists
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        // 2. Validate vendor dispatch batch exists
        VendorDispatchBatch batch = vendorDispatchBatchRepository.findByIdAndTenantIdAndDeletedFalse(batchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor dispatch batch not found with id: " + batchId));

        // 3. Validate vendor dispatch batch status is DISPATCHED
        if (!VendorDispatchBatch.VendorDispatchBatchStatus.DISPATCHED.equals(batch.getVendorDispatchBatchStatus())) {
            log.error("Cannot delete vendor dispatch batch={} as it is not in DISPATCHED status", batchId);
            throw new IllegalStateException("Cannot delete vendor dispatch batch that is not in DISPATCHED status");
        }

        return batch;
    }

    /**
     * Phase 2: Process inventory reversal for processed item
     */
    private void processInventoryReversalForProcessedItem(ProcessedItemVendorDispatchBatch processedItem, Long batchId) {
        Item item = processedItem.getItem();
        Long itemWorkflowId = processedItem.getItemWorkflowId();

        if (itemWorkflowId != null) {
            try {
                validateWorkflowDeletionEligibility(itemWorkflowId, processedItem.getId(), batchId);
                handleInventoryReversalBasedOnWorkflowPosition(processedItem, itemWorkflowId, batchId, item);
            } catch (Exception e) {
                log.warn("Failed to handle workflow pieces reversion for item {}: {}. This may indicate workflow data inconsistency.",
                         item.getId(), e.getMessage());
                throw e;
            }
        } else {
            log.warn("No workflow ID found for item {} during vendor dispatch batch deletion. " +
                     "This may be a legacy record before workflow integration.", item.getId());
            throw new IllegalStateException("No workflow ID found for item " + item.getId());
        }
    }

    /**
     * Validate workflow deletion eligibility
     */
    private void validateWorkflowDeletionEligibility(Long itemWorkflowId, Long entityId, Long batchId) {
        // Use workflow-based validation: check if all entries in next operation are marked deleted
        boolean canDeleteVendor = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, entityId, WorkflowStep.OperationType.VENDOR);

        if (!canDeleteVendor) {
            log.error("Cannot delete vendor dispatch batch id={} as the next operation has active (non-deleted) batches", batchId);
            throw new IllegalStateException("This vendor dispatch batch cannot be deleted as the next operation has active batch entries.");
        }

        log.info("Vendor dispatch batch id={} is eligible for deletion - all next operation batches are deleted", batchId);
    }

    /**
     * Handle inventory reversal based on workflow position (first operation vs subsequent operations)
     */
    private void handleInventoryReversalBasedOnWorkflowPosition(ProcessedItemVendorDispatchBatch processedItem, 
                                                                Long itemWorkflowId, Long batchId, Item item) {
        // Get the workflow to check if vendor was the first operation
        ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
        boolean wasFirstOperation = WorkflowStep.OperationType.VENDOR.equals(
                workflow.getWorkflowTemplate().getFirstStep().getOperationType());

        if (wasFirstOperation) {
            handleHeatInventoryReversalForFirstOperation(processedItem, batchId, item, itemWorkflowId);
        } else {
            handlePiecesReturnToPreviousOperation(processedItem, itemWorkflowId);
        }
    }

    /**
     * Handle heat inventory reversal when vendor was the first operation
     */
    private void handleHeatInventoryReversalForFirstOperation(ProcessedItemVendorDispatchBatch processedItem, 
                                                              Long batchId, Item item, Long itemWorkflowId) {
        // This was the first operation - heat quantities/pieces will be returned to heat inventory
        log.info("Vendor dispatch batch {} was first operation for item {}, heat inventory will be reverted",
                 batchId, item.getId());

        // Update workflow step to mark vendor dispatch batch as deleted and adjust piece counts
        Integer dispatchedPiecesCount = processedItem.getDispatchedPiecesCount();
        if (dispatchedPiecesCount != null && dispatchedPiecesCount > 0) {
            try {
                itemWorkflowService.updateCurrentOperationStepForReturnedPieces(
                        itemWorkflowId, 
                        WorkflowStep.OperationType.VENDOR, 
                        dispatchedPiecesCount,
                        processedItem.getId()
                );
                log.info("Successfully marked vendor operation as deleted and updated workflow step for processed item {}, subtracted {} pieces", 
                         processedItem.getId(), dispatchedPiecesCount);
            } catch (Exception e) {
                log.error("Failed to update workflow step for deleted vendor processed item {}: {}", 
                         processedItem.getId(), e.getMessage());
                throw new RuntimeException("Failed to update workflow step for vendor deletion: " + e.getMessage(), e);
            }
        } else {
            log.info("No dispatched pieces to subtract for deleted processed item {}", processedItem.getId());
        }

        // Note: Material is NOT returned to vendor inventory automatically during deletion
        // This matches the real-world scenario where material consumed for processing stays consumed
        // Material can be returned manually via separate API if needed
        LocalDateTime currentTime = LocalDateTime.now();
        if (processedItem.getVendorDispatchHeats() != null &&
            !processedItem.getVendorDispatchHeats().isEmpty()) {

            Vendor vendor = processedItem.getVendorDispatchBatch().getVendor();

            processedItem.getVendorDispatchHeats().forEach(vendorDispatchHeat -> {
                Heat heat = vendorDispatchHeat.getHeat();
                
                log.info("Material consumed from vendor inventory for heat {} will remain consumed. " +
                         "Use return material API if material needs to be returned to vendor inventory.", 
                         heat.getHeatNumber());

                // Soft delete vendor dispatch heat record
                vendorDispatchHeat.setDeleted(true);
                vendorDispatchHeat.setDeletedAt(currentTime);

                log.info("Processed deletion for vendor dispatch heat: heat={}, vendor={}", 
                         heat.getId(), vendor.getId());
            });
        }
    }

    /**
     * Handle pieces return to previous operation when vendor was not the first operation
     */
    private void handlePiecesReturnToPreviousOperation(ProcessedItemVendorDispatchBatch processedItem, 
                                                       Long itemWorkflowId) {
        // This was not the first operation - return pieces to previous operation
        Long previousOperationBatchId = itemWorkflowService.getPreviousOperationBatchId(
                itemWorkflowId, 
                WorkflowStep.OperationType.VENDOR,
                processedItem.getPreviousOperationProcessedItemId()
        );

        if (previousOperationBatchId != null) {
            itemWorkflowService.returnPiecesToSpecificPreviousOperation(
                    itemWorkflowId,
                    WorkflowStep.OperationType.VENDOR,
                    previousOperationBatchId,
                    processedItem.getDispatchedPiecesCount(),
                    processedItem.getId()
            );

            log.info("Successfully returned {} pieces from vendor back to previous operation {} in workflow {}",
                     processedItem.getDispatchedPiecesCount(),
                     previousOperationBatchId,
                     itemWorkflowId);
        } else {
            log.warn("Could not determine previous operation batch ID for vendor dispatch batch processed item {}. " +
                     "Pieces may not be properly returned to previous operation.", processedItem.getId());
        }
    }

    /**
     * Phase 3: Soft delete processed item and associated records
     */
    private void softDeleteProcessedItemAndAssociatedRecords(VendorDispatchBatch batch, 
                                                             ProcessedItemVendorDispatchBatch processedItem) {
        LocalDateTime now = LocalDateTime.now();
        
        // Soft delete vendor dispatch heats at processed item level
        softDeleteVendorDispatchHeats(processedItem, now);

        // Soft delete processed item vendor dispatch batch
        processedItem.setDeleted(true);
        processedItem.setDeletedAt(now);
    }

    /**
     * Soft delete vendor dispatch heats associated with the processed item
     */
    private void softDeleteVendorDispatchHeats(ProcessedItemVendorDispatchBatch processedItem, LocalDateTime now) {
        if (processedItem.getVendorDispatchHeats() != null && 
            !processedItem.getVendorDispatchHeats().isEmpty()) {
            processedItem.getVendorDispatchHeats().forEach(vendorDispatchHeat -> {
                vendorDispatchHeat.setDeleted(true);
                vendorDispatchHeat.setDeletedAt(now);
            });
        }
    }

    /**
     * Phase 4: Finalize vendor dispatch batch deletion
     */
    private void finalizeVendorDispatchBatchDeletion(VendorDispatchBatch batch, Long batchId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Store the original batch number and modify the batch number for deletion
        batch.setOriginalVendorDispatchBatchNumber(batch.getVendorDispatchBatchNumber());
        batch.setVendorDispatchBatchNumber(batch.getVendorDispatchBatchNumber() + "_deleted_" + batch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC));
        
        // Soft delete vendor dispatch batch
        batch.setDeleted(true);
        batch.setDeletedAt(now);
        vendorDispatchBatchRepository.save(batch);

        log.info("Successfully deleted vendor dispatch batch={}, original batch number={}", batchId, batch.getOriginalVendorDispatchBatchNumber());
    }
} 