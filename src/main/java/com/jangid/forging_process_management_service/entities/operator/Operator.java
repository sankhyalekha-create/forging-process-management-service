package com.jangid.forging_process_management_service.entities.operator;

import com.jangid.forging_process_management_service.entities.Tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operator")
@Inheritance(strategy = InheritanceType.JOINED) // Allows extending for multiple types
@EntityListeners(AuditingEntityListener.class)
public class Operator {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "operator_sequence_generator")
  @SequenceGenerator(name = "operator_sequence_generator", sequenceName = "operator_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "address", nullable = false)
  private String address;

  @Column(name = "aadhaar_number", nullable = false, unique = true)
  private String aadhaarNumber;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @ElementCollection
  @CollectionTable(name = "operator_previous_tenants", joinColumns = @JoinColumn(name = "operator_id"))
  @Column(name = "previous_tenant_id")
  private List<Long> previousTenantIds = new ArrayList<>();

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

  // Method to update tenant and maintain history
  public void updateTenant(Tenant newTenant) {
    if (this.tenant != null) {
      previousTenantIds.add(this.tenant.getId()); // Store previous tenant
    }
    this.tenant = newTenant;
  }
}

