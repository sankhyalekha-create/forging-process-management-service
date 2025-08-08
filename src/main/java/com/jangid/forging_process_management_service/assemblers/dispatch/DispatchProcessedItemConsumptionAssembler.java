package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemConsumption;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchProcessedItemConsumptionRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Assembler for converting between DispatchProcessedItemConsumption entity and its representation.
 */
@Slf4j
@Component
public class DispatchProcessedItemConsumptionAssembler {

    /**
     * Converts DispatchProcessedItemConsumption entity to its representation.
     */
    public DispatchProcessedItemConsumptionRepresentation dissemble(DispatchProcessedItemConsumption consumption) {
        if (consumption == null) {
            return null;
        }

        return DispatchProcessedItemConsumptionRepresentation.builder()
                .id(consumption.getId())
                .previousOperationEntityId(consumption.getPreviousOperationEntityId())
                .previousOperationType(consumption.getPreviousOperationType() != null 
                    ? consumption.getPreviousOperationType().name() 
                    : null)
                .consumedPiecesCount(consumption.getConsumedPiecesCount())
                .availablePiecesCount(consumption.getAvailablePiecesCount())
                .batchIdentifier(consumption.getBatchIdentifier())
                .entityContext(consumption.getEntityContext())
                .build();
    }

    /**
     * Converts DispatchProcessedItemConsumptionRepresentation to entity.
     */
    public DispatchProcessedItemConsumption assemble(DispatchProcessedItemConsumptionRepresentation representation) {
        if (representation == null) {
            return null;
        }

        return DispatchProcessedItemConsumption.builder()
                .id(representation.getId())
                .previousOperationEntityId(representation.getPreviousOperationEntityId())
                .previousOperationType(representation.getPreviousOperationType() != null
                    ? WorkflowStep.OperationType.valueOf(representation.getPreviousOperationType())
                    : null)
                .consumedPiecesCount(representation.getConsumedPiecesCount())
                .availablePiecesCount(representation.getAvailablePiecesCount())
                .batchIdentifier(representation.getBatchIdentifier())
                .entityContext(representation.getEntityContext())
                .build();
    }

    /**
     * Creates a new DispatchProcessedItemConsumption from the provided representation.
     */
    public DispatchProcessedItemConsumption createAssemble(DispatchProcessedItemConsumptionRepresentation representation) {
        DispatchProcessedItemConsumption consumption = assemble(representation);
        if (consumption != null) {
            consumption.setCreatedAt(LocalDateTime.now());
        }
        return consumption;
    }
}