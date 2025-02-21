package com.jangid.forging_process_management_service.entities.product;

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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "product")
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "product",
    indexes = {
        @Index(name = "unique_product_name_active", columnList = "product_name"),
        @Index(name = "unique_product_code_active", columnList = "product_code")
    }
)
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "product_key_sequence_generator")
  @SequenceGenerator(name = "product_key_sequence_generator", sequenceName = "product_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "product_code", nullable = false, unique = true)
  private String productCode;


  @Column(name = "unit_of_measurement", nullable = false)
  private UnitOfMeasurement unitOfMeasurement;

  @ManyToMany
  @JoinTable(
      name = "product_supplier", // Join table name
      joinColumns = @JoinColumn(name = "product_id"),
      inverseJoinColumns = @JoinColumn(name = "supplier_id")
  )
  private List<Supplier> suppliers;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ItemProduct> itemProducts;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;
}
