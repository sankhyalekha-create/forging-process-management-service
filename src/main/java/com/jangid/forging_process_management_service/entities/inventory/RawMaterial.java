package com.jangid.forging_process_management_service.entities.inventory;

import com.jangid.forging_process_management_service.entities.Tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "raw_material")
@EntityListeners(AuditingEntityListener.class)
public class RawMaterial {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "raw_material_key_sequence_generator")
  @SequenceGenerator(name = "raw_material_key_sequence_generator", sequenceName = "raw_material_sequence", allocationSize = 1)
  private long id;

  private LocalDateTime rawMaterialReceivingDate;//mandatory

  @Column(unique = true)
  private String rawMaterialInvoiceNumber;//mandatory
  private float rawMaterialTotalQuantity;//mandatory
  private String rawMaterialInputCode;//mandatory
  private String rawMaterialHsnCode;//mandatory
  private String rawMaterialGoodsDescription;

  @OneToMany(mappedBy = "rawMaterial", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  List<RawMaterialHeat> heats = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

}
