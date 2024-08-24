package com.jangid.forging_process_management_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class RawMaterialHeat {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;
  public String heatNumber; //mandatory
  public float heatQuantity; //mandatory
  public String rawMaterialTestCertificateNumber; //mandatory
  public BarDiameter barDiameter; //mandatory
  public String rawMaterialReceivingInspectionReportNumber; //mandatory
  public String rawMaterialInspectionSource;
  public String rawMaterialLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "raw_material_id")
  public RawMaterial rawMaterial;
}
