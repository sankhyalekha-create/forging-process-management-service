package com.jangid.forging_process_management_service.entities;


import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

public class Bom {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  private long rawMaterialInputCode;
  private String rawMaterialDescription;
  private float rawMaterialInputWeight;
  private float itemForgingWeight;
  private float itemFinishWeight;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "raw_material_id")
  private RawMaterial rawMaterial;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "raw_material_heat_id")
  private RawMaterialHeat heat;

}
