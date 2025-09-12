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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_metadata", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"document_id", "metadata_key"})
})
@EntityListeners(AuditingEntityListener.class)
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "document_metadata_key_sequence_generator")
    @SequenceGenerator(name = "document_metadata_key_sequence_generator", sequenceName = "document_metadata_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @NotNull
    private Document document;

    @Column(name = "metadata_key", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String metadataKey;

    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private String metadataValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_type", length = 20)
    @Builder.Default
    private MetadataType metadataType = MetadataType.STRING;

    @Builder.Default
    private Boolean deleted = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum MetadataType {
        STRING("String"),
        NUMBER("Number"),
        DATE("Date"),
        BOOLEAN("Boolean"),
        JSON("JSON"),
        URL("URL");

        private final String displayName;

        MetadataType(String displayName) {
            this.displayName = displayName;
        }
    }

    // Business logic methods
    public Object getParsedValue() {
        if (metadataValue == null) {
            return null;
        }

        try {
            switch (metadataType) {
                case NUMBER:
                    return Double.parseDouble(metadataValue);
                case BOOLEAN:
                    return Boolean.parseBoolean(metadataValue);
                case DATE:
                    return LocalDateTime.parse(metadataValue);
                case STRING:
                case JSON:
                case URL:
                default:
                    return metadataValue;
            }
        } catch (Exception e) {
            // Return as string if parsing fails
            return metadataValue;
        }
    }

    public void setValue(Object value) {
        if (value == null) {
            this.metadataValue = null;
            return;
        }

        if (value instanceof String) {
            this.metadataValue = (String) value;
            this.metadataType = MetadataType.STRING;
        } else if (value instanceof Number) {
            this.metadataValue = value.toString();
            this.metadataType = MetadataType.NUMBER;
        } else if (value instanceof Boolean) {
            this.metadataValue = value.toString();
            this.metadataType = MetadataType.BOOLEAN;
        } else if (value instanceof LocalDateTime) {
            this.metadataValue = value.toString();
            this.metadataType = MetadataType.DATE;
        } else {
            this.metadataValue = value.toString();
            this.metadataType = MetadataType.STRING;
        }
    }
}
