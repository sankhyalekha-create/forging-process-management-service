package com.jangid.forging_process_management_service.entities.forging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
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

  @Column(name = "furnace_details")
  private String furnaceDetails;

  @Column(name = "furnace_status", nullable = false)
  private String furnaceStatus;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
