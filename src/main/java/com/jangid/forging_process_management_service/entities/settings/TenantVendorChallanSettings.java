package com.jangid.forging_process_management_service.entities.settings;

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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant_vendor_challan_settings", indexes = {
  @Index(name = "idx_vendor_challan_settings_tenant", columnList = "tenant_id"),
  @Index(name = "idx_vendor_challan_settings_active", columnList = "tenant_id, is_active, deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class TenantVendorChallanSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "vendor_challan_settings_sequence_generator")
  @SequenceGenerator(name = "vendor_challan_settings_sequence_generator", 
                    sequenceName = "vendor_challan_settings_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  // Vendor Challan Number Configuration
  @Size(max = 10)
  @Pattern(regexp = "^[A-Z0-9]*$", message = "Prefix must contain only uppercase letters and numbers")
  @Column(name = "challan_prefix", length = 10)
  @Builder.Default
  private String challanPrefix = "VCH";

  @Min(value = 1)
  @Column(name = "start_from")
  @Builder.Default
  private Integer startFrom = 1;

  @Min(value = 1)
  @Max(value = 999999)
  @Column(name = "current_sequence")
  @Builder.Default
  private Integer currentSequence = 1;

  @Size(max = 20)
  @Column(name = "series_format", length = 20)
  @Builder.Default
  private String seriesFormat = "2025-26";

  // Note: Tax configuration (HSN, CGST, SGST, IGST) removed - use TenantInvoiceSettings instead
  // Note: Bank details removed - use TenantInvoiceSettings instead
  // Note: Terms and conditions removed - use TenantInvoiceSettings instead
  // Note: TCS activation removed - use TenantInvoiceSettings instead

  // Status and Audit
  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = true;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Builder.Default
  private boolean deleted = false;

  // Business Methods
  public String getNextChallanNumber() {
    return String.format("%s/%s/%05d", challanPrefix, seriesFormat, currentSequence);
  }

  public void incrementSequence() {
    this.currentSequence++;
  }
}