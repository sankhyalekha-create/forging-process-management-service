package com.jangid.forging_process_management_service.entities.product;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.ProcessedItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "item",
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_item_name_tenant_id_deleted", columnNames = {"itemName", "tenant_id", "deleted"}),
        @UniqueConstraint(name = "unique_item_code_tenant_id_deleted", columnNames = {"item_code", "tenant_id", "deleted"})
    },
    indexes = {
        @Index(name = "idx_item_name_tenant_id", columnList = "itemName, tenant_id"),
        @Index(name = "idx_item_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_item_name", columnList = "itemName"),
        @Index(name = "idx_item_code_tenant_id", columnList = "item_code, tenant_id")
    }
)
public class Item {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "item_key_sequence_generator")
  @SequenceGenerator(name = "item_key_sequence_generator", sequenceName = "item_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "item_name", nullable = false)
  private String itemName;

  @Column(name = "item_code")
  private String itemCode;

  @Column(name = "item_weight")
  private Double itemWeight;

  @Column(name = "item_forged_weight")
  private Double itemForgedWeight;

  @Column(name = "item_finished_weight")
  private Double itemFinishedWeight;

  @Column(name = "item_count")
  private Integer itemCount;

  @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ItemProduct> itemProducts;

  @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProcessedItem> processedItems = new ArrayList<>();

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
  @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tenant"))
  private Tenant tenant;

  public void setItem(ItemProduct itemProduct) {
    itemProduct.setItem(this);
  }

  public void updateItemProducts(List<ItemProduct> itemProducts) {
    if (this.itemProducts != null) {
      this.itemProducts.clear();
    }
    itemProducts.forEach(this::addItemProduct);
  }

  public void addItemProduct(ItemProduct itemProduct) {
    itemProduct.setItem(this);
    if (this.itemProducts != null) {
      this.itemProducts.add(itemProduct);
    }
  }

  /**
   * Validates that the appropriate measurement field is populated based on the UnitOfMeasurement of associated products.
   * For KGS products, all weight fields (itemWeight, itemForgedWeight, and itemFinishedWeight) must be provided.
   * For PIECES products, itemCount is automatically set to 1.
   * All products must have either KGS or PIECES as their unit of measurement.
   * 
   * @return true if valid, false otherwise
   */
  public boolean validateMeasurements() {
    if (itemProducts == null || itemProducts.isEmpty()) {
      return false;
    }
    
    boolean requiresWeight = false;
    boolean hasPiecesProduct = false;
    
    for (ItemProduct itemProduct : itemProducts) {
      Product product = itemProduct.getProduct();
      UnitOfMeasurement uom = product.getUnitOfMeasurement();
      
      if (uom != UnitOfMeasurement.KGS && uom != UnitOfMeasurement.PIECES) {
        // Invalid unit of measurement
        return false;
      }
      
      if (uom == UnitOfMeasurement.KGS) {
        requiresWeight = true;
      } else if (uom == UnitOfMeasurement.PIECES) {
        hasPiecesProduct = true;
      }
    }
    
    // For KGS products, all weight fields must be provided and greater than 0
    if (requiresWeight && ((itemWeight == null || itemWeight <= 0) || (itemForgedWeight == null || itemForgedWeight <= 0) || (itemFinishedWeight == null || itemFinishedWeight <= 0))) {
      return false;
    }
    
    // For PIECES products, always set itemCount to 1
    if (hasPiecesProduct) {
      setItemCount(1);
    }
    
    return true;
  }
}
