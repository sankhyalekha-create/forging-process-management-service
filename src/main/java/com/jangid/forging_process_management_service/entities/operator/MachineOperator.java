package com.jangid.forging_process_management_service.entities.operator;

import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "machine_operator")
public class MachineOperator extends Operator {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "machining_batch_id")
  private MachiningBatch machiningBatch;
}

