package com.jangid.forging_process_management_service.entities.dispatch;

import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Entity representing the consumption of pieces from previous operations for dispatch.
 * This replaces the more specific DispatchProcessedItemInspection to support 
 * consumption from any type of previous operation (not just inspection).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dispatch_processed_item_consumption")
public class DispatchProcessedItemConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "dispatch_processed_item_consumption_sequence_generator")
    @SequenceGenerator(name = "dispatch_processed_item_consumption_sequence_generator", sequenceName = "dispatch_processed_item_consumption_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_batch_id", nullable = false)
    private DispatchBatch dispatchBatch;

    /**
     * ID of the entity from the previous operation (could be ProcessedItemInspectionBatch, 
     * ProcessedItemMachiningBatch, ProcessedItem from forging, etc.)
     */
    @Column(name = "previous_operation_entity_id", nullable = false)
    private Long previousOperationEntityId;

    /**
     * Type of the previous operation (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, VENDOR, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_operation_type", nullable = false)
    private WorkflowStep.OperationType previousOperationType;

    /**
     * Number of pieces consumed from this previous operation entity
     */
    @Column(name = "consumed_pieces_count", nullable = false)
    private Integer consumedPiecesCount;

    /**
     * Number of pieces that were available in the previous operation entity at the time of dispatch
     */
    @Column(name = "available_pieces_count")
    private Integer availablePiecesCount;

    /**
     * Human-readable identifier for the batch (e.g., "InspectionBatch-123", "MachiningBatch-456")
     * Used for display purposes and audit trail
     */
    @Column(name = "batch_identifier")
    private String batchIdentifier;

    /**
     * Additional context information about the consumed entity
     */
    @Column(name = "entity_context", length = 500)
    private String entityContext;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Version
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private boolean deleted;
}