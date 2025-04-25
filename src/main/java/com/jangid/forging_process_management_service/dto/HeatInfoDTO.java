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
    private Double quantity;
    private Integer pieces;
} 