package com.jangid.forging_process_management_service.entities.operator;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "machine_operator")
@PrimaryKeyJoinColumn(name = "id")
public class MachineOperator extends Operator {

  @OneToMany(mappedBy = "machineOperator", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
  private List<DailyMachiningBatch> dailyMachiningBatches = new ArrayList<>();
}

