package com.jangid.forging_process_management_service.entities.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_link")
@EntityListeners(AuditingEntityListener.class)
public class DocumentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "document_link_key_sequence_generator")
    @SequenceGenerator(name = "document_link_key_sequence_generator", sequenceName = "document_link_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @NotNull
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    @NotNull
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    @NotNull
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", length = 50)
    @Builder.Default
    private LinkType linkType = LinkType.ATTACHED;

    @Column(name = "relationship_context", length = 200)
    private String relationshipContext;

    @Builder.Default
    private Boolean deleted = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Getter
    public enum EntityType {
        // Main entities that users will attach documents to
        RAW_MATERIAL("Raw Material"),
        ITEM("Item"),
        PRODUCT("Product"),

        // Order Management
        ORDER("Order"),
        ITEM_WORKFLOW("Item Workflow"),

        // Process Entities
        FORGE("Forge Process"),
        MACHINING_BATCH("Machining Batch"),
        HEAT_TREATMENT_BATCH("Heat Treatment Batch"),
        INSPECTION_BATCH("Inspection Batch"),
        DISPATCH_BATCH("Dispatch Batch"),
        
        // Equipment & People
        FORGING_LINE("Forging Line"),
        MACHINE("Machine"),
        MACHINE_SET("Machine Set"),
        FURNACE("Furnace"),
        INSPECTION_EQUIPMENT("Inspection Equipment"),
        OPERATOR("Operator"),
        
        // External Entities
        VENDOR_DISPATCH_BATCH("Vendor Dispatch Batch"),
        VENDOR_RECEIVE_BATCH("Vendor Receive Batch"),
        BUYER("Buyer"),
        SUPPLIER("Supplier"),
        
        // Organization
        TENANT("Tenant"),
        
        OTHER("Other Entity");

        private final String displayName;

        EntityType(String displayName) {
            this.displayName = displayName;
        }
    }

    @Getter
    public enum LinkType {
        ATTACHED("Attached"),
        REFERENCED("Referenced"),
        DERIVED("Derived"),
        SUPERSEDED("Superseded"),
        RELATED("Related");

        private final String displayName;

        LinkType(String displayName) {
            this.displayName = displayName;
        }
    }

    public boolean isActive() {
        return !deleted;
    }
}
