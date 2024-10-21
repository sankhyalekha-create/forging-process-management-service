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
import jakarta.persistence.SequenceGenerator;
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
@Table(name = "forging_line", indexes = {
    @Index(name = "idx_forging_line_name", columnList = "forging_line_name")
})
@EntityListeners(AuditingEntityListener.class)
public class ForgingLine {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "forging_line_key_sequence_generator")
  @SequenceGenerator(name = "forging_line_key_sequence_generator", sequenceName = "forging_line_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "forging_line_name", nullable = false)
  private String forgingLineName;

  @Column(name = "forging_details")
  private String forgingDetails;

  @Column(name = "forging_status", nullable = false)
  private ForgingLineStatus forgingStatus;

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
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  public enum ForgingLineStatus{
    IDLE,
    IN_PROGRESS,
    COMPLETED;
  }

}

