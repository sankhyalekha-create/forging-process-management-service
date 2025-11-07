package com.jangid.forging_process_management_service.entities.gst;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "challan_line_item", indexes = {
    @Index(name = "idx_challan_line_item_challan", columnList = "delivery_challan_id"),
    @Index(name = "idx_challan_line_item_hsn", columnList = "hsn_code"),
    @Index(name = "idx_challan_line_item_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class ChallanLineItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "challan_line_item_sequence_generator")
  @SequenceGenerator(name = "challan_line_item_sequence_generator", 
                    sequenceName = "challan_line_item_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_challan_id", nullable = false)
  private DeliveryChallan deliveryChallan;

  @NotNull
  @Column(name = "line_number", nullable = false)
  private Integer lineNumber;

  @NotNull
  @Size(max = 500)
  @Column(name = "item_name", nullable = false, length = 500, columnDefinition = "TEXT")
  private String itemName;

  @NotNull
  @Size(max = 10)
  @Column(name = "hsn_code", nullable = false, length = 10)
  private String hsnCode;

  @Size(max = 100)
  @Column(name = "work_type", length = 100)
  private String workType;

  @NotNull
  @Positive
  @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
  private BigDecimal quantity;

  @NotNull
  @Size(max = 10)
  @Column(name = "unit_of_measurement", nullable = false, length = 10)
  private String unitOfMeasurement;

  @Column(name = "rate_per_unit", precision = 15, scale = 2)
  private BigDecimal ratePerUnit;

  // Declared value - ALWAYS required (even for non-taxable challans)
  @NotNull
  @Column(name = "taxable_value", nullable = false, precision = 15, scale = 2)
  private BigDecimal taxableValue;

  @Column(name = "cgst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal cgstRate = BigDecimal.ZERO;

  @Column(name = "sgst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal sgstRate = BigDecimal.ZERO;

  @Column(name = "igst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal igstRate = BigDecimal.ZERO;

  // Tax amounts - OPTIONAL (calculated only when tax is applicable)
  @Column(name = "cgst_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal cgstAmount = BigDecimal.ZERO;

  @Column(name = "sgst_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal sgstAmount = BigDecimal.ZERO;

  @Column(name = "igst_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal igstAmount = BigDecimal.ZERO;

  @NotNull
  @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalValue;

  @Size(max = 500)
  @Column(name = "remarks", length = 500, columnDefinition = "TEXT")
  private String remarks;

  // Traceability - Link back to source data
  @Column(name = "item_workflow_id")
  private Long itemWorkflowId;

  @Column(name = "processed_item_dispatch_batch_id")
  private Long processedItemDispatchBatchId;

  // Standard Audit Fields
  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Builder.Default
  private boolean deleted = false;

  // Business Methods
  
  /**
   * Calculate total value from taxable value and tax amounts
   */
  public void calculateTotalValue() {
    if (taxableValue != null) {
      BigDecimal cgst = cgstAmount != null ? cgstAmount : BigDecimal.ZERO;
      BigDecimal sgst = sgstAmount != null ? sgstAmount : BigDecimal.ZERO;
      BigDecimal igst = igstAmount != null ? igstAmount : BigDecimal.ZERO;
      
      totalValue = taxableValue.add(cgst).add(sgst).add(igst);
    } else {
      totalValue = BigDecimal.ZERO;
    }
  }

  /**
   * Calculate tax amounts based on rates, taxable value, and inter-state flag
   * @param isInterState true if this is an inter-state transaction (use IGST), false for intra-state (use CGST+SGST)
   */
  public void calculateTaxAmounts(boolean isInterState) {
    if (taxableValue == null) {
      return;
    }

    // For inter-state transactions (IGST)
    if (isInterState) {
      if (igstRate != null && igstRate.compareTo(BigDecimal.ZERO) > 0) {
        igstAmount = taxableValue.multiply(igstRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
      } else {
        igstAmount = BigDecimal.ZERO;
      }
      cgstAmount = BigDecimal.ZERO;
      sgstAmount = BigDecimal.ZERO;
    }
    // For intra-state transactions (CGST + SGST)
    else {
      if (cgstRate != null && cgstRate.compareTo(BigDecimal.ZERO) > 0) {
        cgstAmount = taxableValue.multiply(cgstRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
      } else {
        cgstAmount = BigDecimal.ZERO;
      }
      
      if (sgstRate != null && sgstRate.compareTo(BigDecimal.ZERO) > 0) {
        sgstAmount = taxableValue.multiply(sgstRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
      } else {
        sgstAmount = BigDecimal.ZERO;
      }
      
      igstAmount = BigDecimal.ZERO;
    }
  }

  /**
   * Calculate taxable value from quantity and rate
   */
  public void calculateTaxableValue() {
    if (quantity != null && ratePerUnit != null) {
      taxableValue = quantity.multiply(ratePerUnit).setScale(2, java.math.RoundingMode.HALF_UP);
    } else {
      taxableValue = BigDecimal.ZERO;
    }
  }

  /**
   * Validate line item data
   */
  public boolean isValid() {
    if (itemName == null || itemName.trim().isEmpty()) {
      return false;
    }
    if (hsnCode == null || hsnCode.trim().isEmpty()) {
      return false;
    }
    if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    if (unitOfMeasurement == null || unitOfMeasurement.trim().isEmpty()) {
      return false;
    }
    if (taxableValue == null || taxableValue.compareTo(BigDecimal.ZERO) < 0) {
      return false;
    }
    return true;
  }
}

