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
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.EntityListeners;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant_storage_quota")
@EntityListeners(AuditingEntityListener.class)
public class TenantStorageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "tenant_storage_quota_key_sequence_generator")
    @SequenceGenerator(name = "tenant_storage_quota_key_sequence_generator", sequenceName = "tenant_storage_quota_sequence", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    @NotNull
    private Tenant tenant;

    @Column(name = "max_storage_bytes")
    @Min(0)
    @Builder.Default
    private Long maxStorageBytes = 1073741824L; // 1GB default

    @Column(name = "used_storage_bytes")
    @Min(0)
    @Builder.Default
    private Long usedStorageBytes = 0L;

    @Column(name = "max_file_size_bytes")
    @Min(0)
    @Builder.Default
    private Long maxFileSizeBytes = 10485760L; // 10MB default

    @Column(name = "max_files_per_entity")
    @Min(1)
    @Builder.Default
    private Integer maxFilesPerEntity = 100;

    @Column(name = "quota_enabled")
    @Builder.Default
    private Boolean quotaEnabled = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
