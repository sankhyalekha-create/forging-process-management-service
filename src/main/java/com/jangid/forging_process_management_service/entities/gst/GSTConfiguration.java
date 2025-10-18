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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "gst_configuration", indexes = {
    @Index(name = "idx_gst_configuration_tenant", columnList = "tenant_id"),
    @Index(name = "idx_gst_configuration_gstin", columnList = "company_gstin"),
    @Index(name = "idx_gst_configuration_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class GSTConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "gst_configuration_sequence_generator")
    @SequenceGenerator(name = "gst_configuration_sequence_generator", 
                      sequenceName = "gst_configuration_sequence", allocationSize = 1)
    private Long id;

    // Reference
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // Company GST Details (Simplified)
    @NotNull
    @Size(max = 15)
    @Column(name = "company_gstin", nullable = false, length = 15)
    private String companyGstin;

    @NotNull
    @Size(max = 200)
    @Column(name = "company_legal_name", nullable = false, length = 200)
    private String companyLegalName;

    @Size(max = 200)
    @Column(name = "company_trade_name", length = 200)
    private String companyTradeName;

    @NotNull
    @Column(name = "company_address", nullable = false, columnDefinition = "TEXT")
    private String companyAddress;

    @NotNull
    @Size(max = 2)
    @Column(name = "company_state_code", nullable = false, length = 2)
    private String companyStateCode;

    @NotNull
    @Size(max = 6)
    @Column(name = "company_pincode", nullable = false, length = 6)
    private String companyPincode;

    // Invoice Configuration (Simplified)
    @Size(max = 10)
    @Column(name = "invoice_number_prefix", length = 10)
    private String invoiceNumberPrefix;

    @Column(name = "current_invoice_sequence")
    @Builder.Default
    private Integer currentInvoiceSequence = 1;

    // Challan Configuration (Simplified)
    @Size(max = 10)
    @Column(name = "challan_number_prefix", length = 10)
    private String challanNumberPrefix;

    @Column(name = "current_challan_sequence")
    @Builder.Default
    private Integer currentChallanSequence = 1;

    // E-Way Bill Configuration (Simplified)
    @Column(name = "eway_bill_threshold", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ewayBillThreshold = new BigDecimal("50000.00");

    @Column(name = "auto_generate_eway_bill")
    @Builder.Default
    private Boolean autoGenerateEwayBill = true;

    // Default Tax Rates (Simplified)
    @Column(name = "default_cgst_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal defaultCgstRate = new BigDecimal("9.00");

    @Column(name = "default_sgst_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal defaultSgstRate = new BigDecimal("9.00");

    @Column(name = "default_igst_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal defaultIgstRate = new BigDecimal("18.00");

    // Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Standard Audit Fields
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
    public String getNextInvoiceNumber() {
        String prefix = invoiceNumberPrefix != null ? invoiceNumberPrefix : "INV";
        return String.format("%s-%04d", prefix, currentInvoiceSequence);
    }

    public String getNextChallanNumber() {
        String prefix = challanNumberPrefix != null ? challanNumberPrefix : "CHN";
        return String.format("%s-%04d", prefix, currentChallanSequence);
    }

    public void incrementInvoiceSequence() {
        this.currentInvoiceSequence++;
    }

    public void incrementChallanSequence() {
        this.currentChallanSequence++;
    }

    public boolean requiresEwayBill(BigDecimal amount) {
        return amount.compareTo(ewayBillThreshold) >= 0;
    }
}
