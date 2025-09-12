package com.jangid.forging_process_management_service.entities.document;

import com.jangid.forging_process_management_service.entities.Tenant;
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
import jakarta.persistence.Version;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document")
@EntityListeners(AuditingEntityListener.class)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "document_key_sequence_generator")
    @SequenceGenerator(name = "document_key_sequence_generator", sequenceName = "document_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @NotNull
    private Tenant tenant;

    @Column(name = "file_name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    @NotNull
    @Size(min = 1, max = 500)
    private String filePath;

    @Column(name = "compressed_file_path", length = 500)
    private String compressedFilePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String mimeType;

    @Column(name = "file_extension", nullable = false, length = 10)
    @NotNull
    @Size(min = 1, max = 10)
    private String fileExtension;

    @Column(name = "file_size_bytes", nullable = false)
    @NotNull
    private Long fileSizeBytes;

    @Column(name = "compressed_size_bytes")
    private Long compressedSizeBytes;

    @Column(name = "file_hash", nullable = false, length = 64)
    @NotNull
    @Size(min = 1, max = 64)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_category", length = 50)
    private DocumentCategory documentCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50)
    private DocumentType documentType;

    @Column(name = "upload_source", length = 50)
    @Builder.Default
    private String uploadSource = "WEB_UI";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tags", length = 1000)
    private String tags; // Comma-separated tags

    @Column(name = "is_compressed")
    @Builder.Default
    private Boolean isCompressed = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean deleted = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Version
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Relationships
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DocumentMetadata> metadata = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DocumentLink> documentLinks = new ArrayList<>();

    public Double getCompressionRatio() {
        if (compressedSizeBytes != null && fileSizeBytes != null && fileSizeBytes > 0) {
            return ((double) (fileSizeBytes - compressedSizeBytes) / fileSizeBytes) * 100;
        }
        return 0.0;
    }

    public Long getEffectiveFileSize() {
        return isCompressed && compressedSizeBytes != null ? compressedSizeBytes : fileSizeBytes;
    }
}
