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
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice_line_item", indexes = {
  @Index(name = "idx_invoice_line_item_invoice", columnList = "invoice_id"),
  @Index(name = "idx_invoice_line_item_hsn", columnList = "hsn_code"),
  @Index(name = "idx_invoice_line_item_work_type", columnList = "work_type"),
  @Index(name = "idx_invoice_line_item_item_workflow", columnList = "item_workflow_id")
})
@EntityListeners(AuditingEntityListener.class)
public class InvoiceLineItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "invoice_line_item_sequence_generator")
  @SequenceGenerator(name = "invoice_line_item_sequence_generator", 
                    sequenceName = "invoice_line_item_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @NotNull
  @Column(name = "line_number", nullable = false)
  private Integer lineNumber;

  // Item Name
  @NotNull
  @Size(max = 500)
  @Column(name = "item_name", nullable = false, length = 500)
  private String itemName;

  @Size(max = 100)
  @Column(name = "work_type", length = 100)
  private String workType;

  @Size(max = 10)
  @Column(name = "hsn_code", length = 10)
  private String hsnCode;

  // Quantity and Pricing
  @NotNull
  @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
  private BigDecimal quantity;

  @Size(max = 10)
  @Column(name = "unit_of_measurement", length = 10)
  @Builder.Default
  private String unitOfMeasurement = "PCS";

  @NotNull
  @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
  private BigDecimal unitPrice;

  @NotNull
  @Column(name = "taxable_value", precision = 15, scale = 2, nullable = false)
  private BigDecimal taxableValue;

  // GST Details (CGST + SGST for intra-state)
  @Column(name = "cgst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal cgstRate = BigDecimal.ZERO;

  @Column(name = "cgst_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal cgstAmount = BigDecimal.ZERO;

  @Column(name = "sgst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal sgstRate = BigDecimal.ZERO;

  @Column(name = "sgst_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal sgstAmount = BigDecimal.ZERO;

  // GST Details (IGST for inter-state)
  @Column(name = "igst_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal igstRate = BigDecimal.ZERO;

  @Column(name = "igst_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal igstAmount = BigDecimal.ZERO;

  // Discount (optional)
  @Column(name = "discount_percentage", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal discountPercentage = BigDecimal.ZERO;

  @Column(name = "discount_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal discountAmount = BigDecimal.ZERO;

  // Line Total
  @NotNull
  @Column(name = "line_total", precision = 15, scale = 2, nullable = false)
  private BigDecimal lineTotal;

  // Traceability - Link back to source data
  @Column(name = "item_workflow_id")
  private Long itemWorkflowId;

  @Column(name = "processed_item_dispatch_batch_id")
  private Long processedItemDispatchBatchId;

  // Audit Fields
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

  // Business Methods
  
  /**
   * Calculate line total = taxableValue + GST - discount
   */
  public void calculateLineTotal() {
    BigDecimal totalGst = cgstAmount.add(sgstAmount).add(igstAmount);
    this.lineTotal = taxableValue.add(totalGst).subtract(discountAmount);
  }

  /**
   * Calculate taxable value from quantity and unit price
   */
  public void calculateTaxableValue() {
    this.taxableValue = quantity.multiply(unitPrice);
  }

  /**
   * Calculate GST amounts based on rates
   */
  public void calculateGstAmounts() {
    if (cgstRate != null && cgstRate.compareTo(BigDecimal.ZERO) > 0) {
      this.cgstAmount = taxableValue.multiply(cgstRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }
    if (sgstRate != null && sgstRate.compareTo(BigDecimal.ZERO) > 0) {
      this.sgstAmount = taxableValue.multiply(sgstRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }
    if (igstRate != null && igstRate.compareTo(BigDecimal.ZERO) > 0) {
      this.igstAmount = taxableValue.multiply(igstRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }
  }

  /**
   * Calculate discount amount from percentage
   */
  public void calculateDiscountAmount() {
    if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
      this.discountAmount = taxableValue.multiply(discountPercentage)
        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }
  }

  /**
   * Calculate all amounts: taxable value, GST, discount, and line total
   */
  public void calculateAllAmounts() {
    calculateTaxableValue();
    calculateDiscountAmount();
    calculateGstAmounts();
    calculateLineTotal();
  }

  /**
   * Check if this is an inter-state transaction (uses IGST)
   */
  public boolean isInterState() {
    return igstRate != null && igstRate.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Get total GST amount (CGST + SGST + IGST)
   */
  public BigDecimal getTotalGstAmount() {
    return cgstAmount.add(sgstAmount).add(igstAmount);
  }
}

