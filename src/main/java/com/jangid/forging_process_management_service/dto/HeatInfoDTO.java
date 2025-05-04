package com.jangid.forging_process_management_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatInfoDTO {
    private Long heatId;
    private String heatNumber;
    private Double heatQuantity;
    private Double availableHeatQuantity;
    private Double consumedHeatQuantity;
    private Integer piecesCount;
    private Integer availablePiecesCount;
    private Integer consumedPiecesCount;
    
    public Double getConsumedHeatQuantity() {
        return heatQuantity != null && availableHeatQuantity != null 
               ? heatQuantity - availableHeatQuantity 
               : null;
    }
    
    public Integer getConsumedPiecesCount() {
        return piecesCount != null && availablePiecesCount != null 
               ? piecesCount - availablePiecesCount 
               : null;
    }
} 