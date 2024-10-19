package com.jangid.forging_process_management_service.entities.forging;

import com.jangid.forging_process_management_service.entities.Tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "furnace", indexes = {
    @Index(name = "idx_furnace_name", columnList = "furnace_name")
})
@EntityListeners(AuditingEntityListener.class)
public class Furnace {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "furnace_name", nullable = false)
  private String furnaceName;

  @Column(name = "furnace_capacity", nullable = false)
  private Float furnaceCapacity;

  @Column(name = "furnace_location")
  private String furnaceLocation;

  @Column(name = "furnace_details")
  private String furnaceDetails;

  @Column(name = "furnace_status", nullable = false)
  private FurnaceStatus furnaceStatus;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  public enum FurnaceStatus{
    IDLE,
    IN_PROGRESS,
    COMPLETED;
  }
}
