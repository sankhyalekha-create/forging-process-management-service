package com.jangid.forging_process_management_service.entities.inventory;

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
  private long id;
  public String heatNumber; //mandatory
  public double heatQuantity; //mandatory
  public double availableHeatQuantity; //mandatory
  public String testCertificateNumber; //mandatory
  public String location;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "raw_material_product_id", nullable = false)
  private RawMaterialProduct rawMaterialProduct;
}
