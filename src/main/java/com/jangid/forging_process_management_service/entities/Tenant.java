package com.jangid.forging_process_management_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Tenant {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;
  private String tenantName;
  @Column(name="tenant_org_id", unique = true)
  private String tenantOrgId;
  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  @Version
  private Date updatedAt;

  private Date deletedAt;

  private boolean deleted;

}
