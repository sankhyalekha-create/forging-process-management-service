package com.jangid.forging_process_management_service.entities.dispatch;

import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "processed_item_dispatch_batch")
public class ProcessedItemDispatchBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "processed_item_dispatch_batch_sequence_generator")
  @SequenceGenerator(name = "processed_item_dispatch_batch_sequence_generator", sequenceName = "processed_item_dispatch_batch_sequence", allocationSize = 1)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispatch_batch_id", nullable = false)
  private DispatchBatch dispatchBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_id", nullable = false)
  private ProcessedItem processedItem;

  @Column(name = "total_dispatch_pieces_count")
  private Integer totalDispatchPiecesCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_status", nullable = false)
  private ItemStatus itemStatus;

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

}
