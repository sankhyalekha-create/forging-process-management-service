package com.jangid.forging_process_management_service.entities.dispatch;

import com.jangid.forging_process_management_service.entities.inventory.Heat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "dispatch_heat")
public class DispatchHeat {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "dispatch_heat_key_sequence_generator")
  @SequenceGenerator(name = "dispatch_heat_key_sequence_generator", sequenceName = "dispatch_heat_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_dispatch_batch_id", nullable = false)
  private ProcessedItemDispatchBatch processedItemDispatchBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "heat_id", nullable = false)
  private Heat heat;

  @Column(name = "pieces_used", nullable = false)
  private Integer piecesUsed;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;
} 