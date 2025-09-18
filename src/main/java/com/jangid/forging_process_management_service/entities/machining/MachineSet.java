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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "machine_set", indexes = {
    @Index(name = "idx_machine_set_name", columnList = "machine_set_name"),
    @Index(name = "idx_machine_set_tenant", columnList = "tenant_id")
}
    // Note: Uniqueness for active records handled by partial index in database migration V1_57
)
@EntityListeners(AuditingEntityListener.class)
public class MachineSet {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "machine_set_key_sequence_generator")
  @SequenceGenerator(name = "machine_set_key_sequence_generator", sequenceName = "machine_set_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "machine_set_name", nullable = false)
  private String machineSetName;

  @Column(name = "machine_set_description")
  private String machineSetDescription;

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

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "machine_set_machine",
      joinColumns = @JoinColumn(name = "machine_set_id"),
      inverseJoinColumns = @JoinColumn(name = "machine_id")
  )
  private Set<Machine> machines;

  @Enumerated(EnumType.STRING)
  @Column(name = "machine_set_status", nullable = false)
  private MachineSetStatus machineSetStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "machine_set_running_job_type", nullable = false)
  private MachineSetRunningJobType machineSetRunningJobType;

  public enum MachineSetStatus{
    MACHINING_NOT_APPLIED,
    MACHINING_APPLIED,
    MACHINING_IN_PROGRESS;
  }

  public enum MachineSetRunningJobType{
    NONE,
    FRESH,
    REWORK;
  }
}
