package com.jangid.forging_process_management_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithHeatsDTO {
    private Long productId;
    private String productName;
    private String productCode;
    private List<HeatInfoDTO> heats;

    // Constructor for JPQL projection
    public ProductWithHeatsDTO(Long productId, String productName, String productCode) {
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
    }
} 