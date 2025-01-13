package com.jangid.forging_process_management_service.entities.machining;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "machine", indexes = {
    @Index(name = "idx_machine_name", columnList = "machine_name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_machine_name_tenant", columnNames = {"machine_name", "tenant_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class Machine {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "machine_key_sequence_generator")
  @SequenceGenerator(name = "machine_key_sequence_generator", sequenceName = "machine_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "machine_name", nullable = false)
  private String machineName;

  @Column(name = "machine_location")
  private String machineLocation;

  @Column(name = "machine_details")
  private String machineDetails;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;
}
