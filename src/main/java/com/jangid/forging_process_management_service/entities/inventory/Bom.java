package com.jangid.forging_process_management_service.entities.inventory;


import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

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
