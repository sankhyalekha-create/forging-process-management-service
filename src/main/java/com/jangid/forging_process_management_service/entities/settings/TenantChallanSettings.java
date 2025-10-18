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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant_challan_settings", indexes = {
  @Index(name = "idx_challan_settings_tenant", columnList = "tenant_id"),
  @Index(name = "idx_challan_settings_active", columnList = "tenant_id, is_active, deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class TenantChallanSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "challan_settings_sequence_generator")
  @SequenceGenerator(name = "challan_settings_sequence_generator", 
                    sequenceName = "challan_settings_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  // Challan Number Configuration
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

  // Tax Configuration
  @Size(max = 10)
  @Pattern(regexp = "^[0-9]{6,8}$", message = "HSN/SAC code must be 6-8 digits")
  @Column(name = "hsn_sac_code", length = 10)
  private String hsnSacCode;

  @DecimalMin(value = "0.00", message = "CGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "CGST rate cannot exceed 50%")
  @Column(name = "cgst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal cgstRate = new BigDecimal("9.00");

  @DecimalMin(value = "0.00", message = "SGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "SGST rate cannot exceed 50%")
  @Column(name = "sgst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal sgstRate = new BigDecimal("9.00");

  @DecimalMin(value = "0.00", message = "IGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "IGST rate cannot exceed 50%")
  @Column(name = "igst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal igstRate = new BigDecimal("18.00");

  @Column(name = "activate_tcs")
  @Builder.Default
  private Boolean activateTCS = false;

  // Bank Details
  @Column(name = "bank_details_same_as_jobwork")
  @Builder.Default
  private Boolean bankDetailsSameAsJobwork = true;

  @Size(max = 100)
  @Column(name = "bank_name", length = 100)
  private String bankName;

  @Size(max = 20)
  @Column(name = "account_number", length = 20)
  private String accountNumber;

  @Size(max = 11)
  @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
  @Column(name = "ifsc_code", length = 11)
  private String ifscCode;

  // Terms and Conditions
  @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
  private String termsAndConditions;

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
    return String.format("%s-%04d", seriesFormat, currentSequence);
  }

  public void incrementSequence() {
    this.currentSequence++;
  }

  public BigDecimal getTotalGSTRate() {
    return cgstRate.add(sgstRate);
  }
}
