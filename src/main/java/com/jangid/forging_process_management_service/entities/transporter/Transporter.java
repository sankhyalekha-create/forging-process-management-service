package com.jangid.forging_process_management_service.entities.transporter;

import com.jangid.forging_process_management_service.entities.Tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * Entity representing a Transporter in the system.
 * This entity stores transporter information required for GST-compliant invoice generation
 * and E-Way Bill processing.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transporter", indexes = {
  @Index(name = "idx_transporter_name", columnList = "transporter_name"),
  @Index(name = "idx_transporter_gstin", columnList = "gstin"),
  @Index(name = "idx_transporter_id_number", columnList = "transporter_id_number")
  // Note: Uniqueness for active records handled by partial index in database migration
})
public class Transporter {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  /**
   * Legal/registered name of the transporter
   */
  @Column(name = "transporter_name", nullable = false)
  private String transporterName;
  
  /**
   * GST Identification Number (15 digits)
   * Format: 22ABCDE1234F1Z5
   */
  @Column(name = "gstin", length = 15)
  private String gstin;
  
  /**
   * Transporter ID issued by GST portal
   * This is the 15-digit GSTIN or 14-digit TIN for unregistered transporters
   * Required for E-Way Bill generation
   */
  @Column(name = "transporter_id_number", length = 15)
  private String transporterIdNumber;
  
  /**
   * PAN card number of the transporter
   * Format: ABCDE1234F
   */
  @Column(name = "pan_number", length = 10)
  private String panNumber;
  
  /**
   * Complete registered address
   */
  @Column(name = "address", columnDefinition = "TEXT")
  private String address;
  
  /**
   * State code as per GST (2 digits)
   * Examples: "09" for Uttar Pradesh, "08" for Rajasthan
   */
  @Column(name = "state_code", length = 2)
  private String stateCode;
  
  /**
   * PIN code (6 digits)
   */
  @Column(name = "pincode", length = 6)
  private String pincode;
  
  /**
   * Primary contact phone number
   * Format: 10-digit mobile or landline with STD code
   */
  @Column(name = "phone_number", length = 15)
  private String phoneNumber;
  
  /**
   * Alternate contact phone number
   */
  @Column(name = "alternate_phone_number", length = 15)
  private String alternatePhoneNumber;
  
  /**
   * Email address
   */
  @Column(name = "email")
  private String email;
  
  /**
   * Whether the transporter is registered under GST
   */
  @Column(name = "is_gst_registered")
  private boolean isGstRegistered;
  
  /**
   * Bank account number for payment processing
   * Maximum 18 characters as per GST E-Invoice requirements
   */
  @Size(max = 18)
  @Column(name = "bank_account_number", length = 18)
  private String bankAccountNumber;
  
  /**
   * IFSC code of the bank
   */
  @Column(name = "ifsc_code", length = 11)
  private String ifscCode;
  
  /**
   * Bank name
   */
  @Column(name = "bank_name")
  private String bankName;
  
  /**
   * Additional notes or remarks about the transporter
   */
  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;
  
  // Audit fields
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
  
  // Multi-tenant relationship
  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;
}

