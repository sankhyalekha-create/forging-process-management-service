package com.jangid.forging_process_management_service.entities.inventory;

import com.jangid.forging_process_management_service.utils.PrecisionUtils;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "heat")
@EntityListeners(AuditingEntityListener.class)
public class Heat {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "heat_key_sequence_generator")
  @SequenceGenerator(name = "heat_key_sequence_generator", sequenceName = "heat_sequence", allocationSize = 1)
  private Long id;
  private String heatNumber; //mandatory
  private Double heatQuantity; //mandatory
  private Double availableHeatQuantity; //mandatory

  @Column(name = "is_in_pieces", nullable = false)
  private Boolean isInPieces;

  @Column(name = "pieces_count")
  private Integer piecesCount;
  
  @Column(name = "available_pieces_count")
  private Integer availablePiecesCount;

  private String testCertificateNumber; //mandatory
  private String location;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  @Column(name = "active", nullable = false)
  private Boolean active = true;  // Default to active for existing data

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "raw_material_product_id")
  private RawMaterialProduct rawMaterialProduct;
  
  /**
   * Consumes a specified quantity from this heat
   * @param quantity The quantity to consume
   * @return true if successful, false if insufficient quantity
   */
  public boolean consumeQuantity(Double quantity) {
    if (isInPieces) {
      int piecesToConsume = quantity.intValue();
      if (availablePiecesCount >= piecesToConsume) {
        availablePiecesCount -= piecesToConsume;
        return true;
      }
    } else {
      if (availableHeatQuantity >= quantity) {
        // Round the result to maintain 4-decimal precision
        availableHeatQuantity = PrecisionUtils.roundQuantity(availableHeatQuantity - quantity);
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns the available quantity based on the unit of measurement
   */
  public Double getAvailableQuantity() {
    if (isInPieces) {
      return (double) availablePiecesCount;
    } else {
      return availableHeatQuantity;
    }
  }
  
  /**
   * Returns the total quantity based on the unit of measurement
   */
  public Double getTotalQuantity() {
    if (isInPieces) {
      return (double) piecesCount;
    } else {
      return heatQuantity;
    }
  }
}
