package com.jangid.forging_process_management_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Item {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  private long itemCode;

//  @NotNull
//  @OneToMany(fetch = FetchType.LAZY)
//  @JoinColumn(name = "bom_id")
//  private Bom bomId;

  private String heatTreatmentProcess;
  private String customerName;
  private ItemStatus status;
}
