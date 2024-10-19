package com.jangid.forging_process_management_service.entities.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class RawMaterialInspectionReport {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;
  public String rawMaterialInspectionReportNumber;
  public Date rawMaterialInspectionReportDate;
  public String rawMaterialInspectionReportSource;

}
