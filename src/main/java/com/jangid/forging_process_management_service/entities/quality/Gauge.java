package com.jangid.forging_process_management_service.entities.quality;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gauge", indexes = {
    @Index(name = "idx_gauge_name", columnList = "gauge_name")
}
    // Note: Uniqueness for active records handled by partial index in database migration V1_52
)
@EntityListeners(AuditingEntityListener.class)
public class Gauge {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "gauge_key_sequence_generator")
  @SequenceGenerator(name = "gauge_key_sequence_generator", sequenceName = "gauge_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "gauge_name", nullable = false)
  private String gaugeName;

  @Column(name = "gauge_location")
  private String gaugeLocation;

  @Column(name = "gauge_details")
  private String gaugeDetails;

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
}
