package com.jangid.forging_process_management_service.entities.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
