package com.jangid.forging_process_management_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Tenant {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "tenant_key_sequence_generator")
  @SequenceGenerator(name = "tenant_key_sequence_generator", sequenceName = "tenant_sequence")
  private long id;

  @NotNull
  private String tenantName;

  @NotNull
  @Column(name="tenant_org_id", unique = true)
  private String tenantOrgId; // 30 character alphanumric string + @forgingorg

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

}
