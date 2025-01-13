package com.jangid.forging_process_management_service.entities.inventory;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.product.Supplier;

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
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "raw_material")
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "raw_material",
    uniqueConstraints = @UniqueConstraint(name = "unique_raw_material_invoice_tenant", columnNames = {"rawMaterialInvoiceNumber", "tenant_id"}),
    indexes = {
        @Index(name = "idx_raw_material_invoice_number_tenant_id", columnList = "rawMaterialInvoiceNumber, tenant_id"),
        @Index(name = "idx_raw_material_hsn_code", columnList = "rawMaterialHsnCode"),
        @Index(name = "idx_raw_material_tenant_id", columnList = "tenant_id")
    }
)
public class RawMaterial {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "raw_material_key_sequence_generator")
  @SequenceGenerator(name = "raw_material_key_sequence_generator", sequenceName = "raw_material_sequence", allocationSize = 1)
  private Long id;

  private LocalDateTime rawMaterialInvoiceDate;

  @Column(name = "po_number")
  private String poNumber;

  @Column(name = "raw_material_receiving_date", nullable = false)
  private LocalDateTime rawMaterialReceivingDate;

  @Column(name = "raw_material_invoice_number", nullable = false)
  private String rawMaterialInvoiceNumber;

  @Column(name = "raw_material_total_quantity", nullable = false)
  private Double rawMaterialTotalQuantity;

  @Column(name = "raw_material_hsn_code", nullable = false)
  private String rawMaterialHsnCode;

  @Column(name = "raw_material_goods_description")
  private String rawMaterialGoodsDescription;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false, foreignKey = @ForeignKey(name = "fk_raw_material_supplier"))
  private Supplier supplier;

  @OneToMany(mappedBy = "rawMaterial", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RawMaterialProduct> rawMaterialProducts = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "deleted", nullable = false)
  private boolean deleted;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_raw_material_tenant"))
  private Tenant tenant;

  public void setRawMaterial(RawMaterialProduct rawMaterialProduct) {
    rawMaterialProduct.setRawMaterial(this);
  }

  public void updateRawMaterialProducts(List<RawMaterialProduct> rawMaterialProducts) {
    if (this.rawMaterialProducts != null) {
      this.rawMaterialProducts.clear();
    }
    rawMaterialProducts.forEach(this::addRawMaterialProduct);
  }

  public void addRawMaterialProduct(RawMaterialProduct rawMaterialProduct) {
    rawMaterialProduct.setRawMaterial(this);
    if (this.rawMaterialProducts != null) {
      this.rawMaterialProducts.add(rawMaterialProduct);
    }
  }
}
