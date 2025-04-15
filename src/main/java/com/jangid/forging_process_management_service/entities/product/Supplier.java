package com.jangid.forging_process_management_service.entities.product;

import com.jangid.forging_process_management_service.entities.Tenant;

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
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "supplier",
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_supplier_name_tenant_deleted", columnNames = {"supplier_name", "tenant_id", "deleted"})
    },
    indexes = {
        @Index(name = "idx_supplier_name_tenant_id", columnList = "supplier_name, tenant_id"),
        @Index(name = "idx_supplier_supplier_name", columnList = "supplier_name")
    }
)
@Entity(name = "supplier")
@EntityListeners(AuditingEntityListener.class)
public class Supplier {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "supplier_key_sequence_generator")
  @SequenceGenerator(name = "supplier_key_sequence_generator", sequenceName = "supplier_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "supplier_name", nullable = false)
  private String supplierName;

  @Column(name = "supplier_detail")
  private String supplierDetail;

  @Column(name = "phone_number", length = 15)
  private String phoneNumber;

  @Column(name = "pan_number", length = 10)
  private String panNumber;

  @Column(name = "gstin_number", length = 15)
  private String gstinNumber;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_supplier_tenant"))
  private Tenant tenant;

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "deleted", nullable = false)
  private boolean deleted;
}

