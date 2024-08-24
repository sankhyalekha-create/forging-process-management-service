package com.jangid.forging_process_management_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.util.Date;


import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Furnace {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;
  private String furnaceName;

  @NotNull
  private float furnaceCapacity;
  private String description;
  private String location;
  private Status status;

  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  @Version
  private Date updatedAt;

  private Date deletedAt;

  private boolean deleted;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  public enum Status{
    IN_PROGRESS,
    COMPLETED;
  }

}
