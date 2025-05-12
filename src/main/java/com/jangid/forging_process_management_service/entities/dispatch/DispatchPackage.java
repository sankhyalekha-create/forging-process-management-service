package com.jangid.forging_process_management_service.entities.dispatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dispatch_package")
@EntityListeners(AuditingEntityListener.class)
public class DispatchPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "dispatch_package_sequence_generator")
    @SequenceGenerator(name = "dispatch_package_sequence_generator", sequenceName = "dispatch_package_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_batch_id", nullable = false)
    private DispatchBatch dispatchBatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_type", nullable = false)
    private DispatchBatch.PackagingType packagingType;

    @Column(name = "quantity_in_package", nullable = false)
    private Integer quantityInPackage;

    @Column(name = "package_number")
    private Integer packageNumber;

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