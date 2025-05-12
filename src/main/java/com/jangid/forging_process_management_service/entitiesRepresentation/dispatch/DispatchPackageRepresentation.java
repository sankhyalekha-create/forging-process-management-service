package com.jangid.forging_process_management_service.entitiesRepresentation.dispatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchPackageRepresentation {
    private Long id;
    private String packagingType;
    private Integer quantityInPackage;
    private Integer packageNumber;
} 