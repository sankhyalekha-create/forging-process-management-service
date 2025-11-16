package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "challan_vendor_line_item", indexes = {
    @Index(name = "idx_challan_vendor_line_item_challan", columnList = "delivery_challan_id"),
    @Index(name = "idx_challan_vendor_line_item_vendor_dispatch", columnList = "vendor_dispatch_batch_id"),
    @Index(name = "idx_challan_vendor_line_item_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class ChallanVendorLineItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "challan_vendor_line_item_sequence_generator")
  @SequenceGenerator(name = "challan_vendor_line_item_sequence_generator",
                     sequenceName = "challan_vendor_line_item_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_challan_id", nullable = false)
  private DeliveryChallan deliveryChallan;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vendor_dispatch_batch_id", nullable = false)
  private VendorDispatchBatch vendorDispatchBatch;

  @Column(name = "line_number")
  private Integer lineNumber;

  @Column(name = "item_name")
  private String itemName;

  @Column(name = "hsn_code")
  private String hsnCode;

  @Column(name = "work_type")
  private String workType;

  @Column(name = "quantity", precision = 19, scale = 4)
  private BigDecimal quantity;

  @Column(name = "unit_of_measurement")
  private String unitOfMeasurement;

  @Column(name = "rate_per_unit", precision = 19, scale = 4)
  private BigDecimal ratePerUnit;

  @Column(name = "taxable_value", precision = 19, scale = 4)
  private BigDecimal taxableValue;

  @Column(name = "cgst_rate", precision = 19, scale = 4)
  private BigDecimal cgstRate;

  @Column(name = "cgst_amount", precision = 19, scale = 4)
  private BigDecimal cgstAmount;

  @Column(name = "sgst_rate", precision = 19, scale = 4)
  private BigDecimal sgstRate;

  @Column(name = "sgst_amount", precision = 19, scale = 4)
  private BigDecimal sgstAmount;

  @Column(name = "igst_rate", precision = 19, scale = 4)
  private BigDecimal igstRate;

  @Column(name = "igst_amount", precision = 19, scale = 4)
  private BigDecimal igstAmount;

  @Column(name = "total_value", precision = 19, scale = 4)
  private BigDecimal totalValue;

  @Column(name = "remarks")
  private String remarks;

  // Traceability - Link back to source data
  @Column(name = "item_workflow_id")
  private Long itemWorkflowId;

  @Column(name = "processed_item_vendor_dispatch_batch_id")
  private Long processedItemVendorDispatchBatchId;

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

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  private boolean deleted;

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
