package com.jangid.forging_process_management_service.entities;

public enum BarDiameter {

  FORTYMM("40mm");

  public final String label;

  private BarDiameter(String label) {
    this.label = label;
  }
}
