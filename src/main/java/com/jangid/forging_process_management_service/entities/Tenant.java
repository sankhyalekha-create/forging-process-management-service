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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "tenant")
@EntityListeners(AuditingEntityListener.class)
public class Tenant {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "tenant_key_sequence_generator")
  @SequenceGenerator(name = "tenant_key_sequence_generator", sequenceName = "tenant_sequence", allocationSize = 1)
  private long id;

  @NotNull
  private String tenantName;

  @NotNull
  @Column(name="tenant_org_id", unique = true)
  private String tenantOrgId; // 30 character alphanumric string + @forgingorg

  @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
  @Column(name = "phone_number")
  private String phoneNumber;

  @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
  @Column(unique = true)
  private String gstin;

  @Email(message = "Invalid email format")
  @Column(unique = true)
  private String email;

  @Column(length = 500)
  private String address;

  @Column(name = "other_details", length = 1000)
  private String otherDetails;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;
}
