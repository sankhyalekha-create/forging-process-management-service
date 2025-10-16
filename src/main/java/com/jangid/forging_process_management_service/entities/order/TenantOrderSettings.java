package com.jangid.forging_process_management_service.entities.order;

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
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant_order_settings")
@EntityListeners(AuditingEntityListener.class)
public class TenantOrderSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "tenant_order_settings_sequence")
  @SequenceGenerator(name = "tenant_order_settings_sequence", sequenceName = "tenant_order_settings_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false, unique = true)
  private Tenant tenant;

  // Highlighting Settings
  @Column(name = "warning_days", nullable = false)
  @Builder.Default
  @Min(1)
  @Max(14)
  private Integer warningDays = 3;

  @Column(name = "enable_highlighting", nullable = false)
  @Builder.Default
  private Boolean enableHighlighting = true;

  @Column(name = "overdue_color", length = 20)
  @Builder.Default
  private String overdueColor = "#ffebee";

  @Column(name = "warning_color", length = 20)
  @Builder.Default
  private String warningColor = "#fff8e1";

  @Column(name = "completed_color", length = 20)
  @Builder.Default
  private String completedColor = "#e8f5e9";

  // Display Settings
  @Column(name = "auto_refresh_interval", nullable = false)
  @Builder.Default
  @Min(10)
  @Max(300)
  private Integer autoRefreshInterval = 30; // seconds

  @Column(name = "enable_notifications", nullable = false)
  @Builder.Default
  private Boolean enableNotifications = true;

  @Column(name = "show_completed_orders", nullable = false)
  @Builder.Default
  private Boolean showCompletedOrders = true;

  @Column(name = "default_priority", nullable = false)
  @Builder.Default
  @Min(1)
  @Max(5)
  private Integer defaultPriority = 3;

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}

