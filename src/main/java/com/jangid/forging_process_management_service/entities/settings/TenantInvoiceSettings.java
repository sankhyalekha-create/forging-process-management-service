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
@Table(name = "tenant_invoice_settings", indexes = {
  @Index(name = "idx_invoice_settings_tenant", columnList = "tenant_id"),
  @Index(name = "idx_invoice_settings_active", columnList = "tenant_id, is_active, deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class TenantInvoiceSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "invoice_settings_sequence_generator")
  @SequenceGenerator(name = "invoice_settings_sequence_generator", 
                    sequenceName = "invoice_settings_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  // Job Work Invoice Settings
  @Size(max = 10)
  @Pattern(regexp = "^[A-Z0-9]*$", message = "Prefix must contain only uppercase letters and numbers")
  @Column(name = "job_work_invoice_prefix", length = 10)
  @Builder.Default
  private String jobWorkInvoicePrefix = "JW";

  @Min(value = 1, message = "Current sequence must be at least 1")
  @Max(value = 999999, message = "Current sequence cannot exceed 999999")
  @Column(name = "job_work_current_sequence")
  @Builder.Default
  private Integer jobWorkCurrentSequence = 1;

  @Size(max = 20)
  @Column(name = "job_work_series_format", length = 20)
  private String jobWorkSeriesFormat;

  @Min(value = 1)
  @Column(name = "job_work_start_from")
  @Builder.Default
  private Integer jobWorkStartFrom = 1;

  // Job Work Tax Settings
  @Size(max = 10)
  @Pattern(regexp = "^[0-9]{6,8}$", message = "HSN/SAC code must be 6-8 digits")
  @Column(name = "job_work_hsn_sac_code", length = 10)
  private String jobWorkHsnSacCode;

  @DecimalMin(value = "0.00", message = "CGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "CGST rate cannot exceed 50%")
  @Column(name = "job_work_cgst_rate", precision = 5, scale = 2)
  private BigDecimal jobWorkCgstRate;

  @DecimalMin(value = "0.00", message = "SGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "SGST rate cannot exceed 50%")
  @Column(name = "job_work_sgst_rate", precision = 5, scale = 2)
  private BigDecimal jobWorkSgstRate;

  @DecimalMin(value = "0.00", message = "IGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "IGST rate cannot exceed 50%")
  @Column(name = "job_work_igst_rate", precision = 5, scale = 2)
  private BigDecimal jobWorkIgstRate;

  // Material Invoice Settings  
  @Size(max = 10)
  @Pattern(regexp = "^[A-Z0-9]*$", message = "Prefix must contain only uppercase letters and numbers")
  @Column(name = "material_invoice_prefix", length = 10)
  private String materialInvoicePrefix;

  @Min(value = 1)
  @Max(value = 999999)
  @Column(name = "material_current_sequence")
  @Builder.Default
  private Integer materialCurrentSequence = 1;

  @Size(max = 20)
  @Column(name = "material_series_format", length = 20)
  private String materialSeriesFormat;

  @Min(value = 1)
  @Column(name = "material_start_from")
  @Builder.Default
  private Integer materialStartFrom = 1;

  // Material Tax Settings
  @Size(max = 10)
  @Pattern(regexp = "^[0-9]{6,8}$", message = "HSN/SAC code must be 6-8 digits")
  @Column(name = "material_hsn_sac_code", length = 10)
  private String materialHsnSacCode;

  @DecimalMin(value = "0.00", message = "CGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "CGST rate cannot exceed 50%")
  @Column(name = "material_cgst_rate", precision = 5, scale = 2)
  private BigDecimal materialCgstRate;

  @DecimalMin(value = "0.00", message = "SGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "SGST rate cannot exceed 50%")
  @Column(name = "material_sgst_rate", precision = 5, scale = 2)
  private BigDecimal materialSgstRate;

  @DecimalMin(value = "0.00", message = "IGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "IGST rate cannot exceed 50%")
  @Column(name = "material_igst_rate", precision = 5, scale = 2)
  private BigDecimal materialIgstRate;

  // Manual Invoice Settings
  @Column(name = "manual_invoice_enabled")
  private Boolean manualInvoiceEnabled;

  @Size(max = 100)
  @Column(name = "manual_invoice_title", length = 100)
  @Builder.Default
  private String manualInvoiceTitle = "GST INVOICE";

  @Min(value = 1)
  @Column(name = "manual_start_from")
  @Builder.Default
  private Integer manualStartFrom = 1;

  // Integration Settings (reduced)

  @Column(name = "show_vehicle_number")
  @Builder.Default
  private Boolean showVehicleNumber = true;

  // E-Way Bill & E-Invoice Settings
  @Column(name = "activate_eway_bill")
  @Builder.Default
  private Boolean activateEWayBill = false;

  @Column(name = "activate_einvoice")
  @Builder.Default
  private Boolean activateEInvoice = false;

  @Column(name = "activate_tcs")
  @Builder.Default
  private Boolean activateTCS = false;

  // Transport & Bank Details
  @Column(name = "transporter_details")
  @Builder.Default
  private Boolean transporterDetails = true;

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

  // Terms and Conditions - Separate for each work type
  @Column(name = "job_work_terms_and_conditions", columnDefinition = "TEXT")
  private String jobWorkTermsAndConditions;

  @Column(name = "material_terms_and_conditions", columnDefinition = "TEXT")
  private String materialTermsAndConditions;

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
  public String getNextJobWorkInvoiceNumber() {
    return String.format("%s%s-%04d", jobWorkInvoicePrefix, jobWorkSeriesFormat, jobWorkCurrentSequence);
  }

  public String getNextMaterialInvoiceNumber() {
    return String.format("%s%s-%04d", materialInvoicePrefix, materialSeriesFormat, materialCurrentSequence);
  }

  public void incrementJobWorkSequence() {
    this.jobWorkCurrentSequence++;
  }

  public void incrementMaterialSequence() {
    this.materialCurrentSequence++;
  }

  // Tax calculation methods
  
  // Job Work Tax Calculations
  public BigDecimal getJobWorkTotalGSTRate() {
    if (jobWorkCgstRate != null && jobWorkSgstRate != null) {
      return jobWorkCgstRate.add(jobWorkSgstRate);
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getJobWorkTotalTaxRate() {
    return jobWorkIgstRate != null ? jobWorkIgstRate : getJobWorkTotalGSTRate();
  }
  
  // Material Tax Calculations
  public BigDecimal getMaterialTotalGSTRate() {
    if (materialCgstRate != null && materialSgstRate != null) {
      return materialCgstRate.add(materialSgstRate);
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getMaterialTotalTaxRate() {
    return materialIgstRate != null ? materialIgstRate : getMaterialTotalGSTRate();
  }
}
