package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchPackage;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchPackageRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchPackageAssembler {

    /**
     * Converts DispatchPackage to DispatchPackageRepresentation.
     */
    public DispatchPackageRepresentation dissemble(DispatchPackage dispatchPackage) {
        return DispatchPackageRepresentation.builder()
            .id(dispatchPackage.getId())
            .packagingType(dispatchPackage.getPackagingType() != null ? dispatchPackage.getPackagingType().name() : null)
            .quantityInPackage(dispatchPackage.getQuantityInPackage())
            .packageNumber(dispatchPackage.getPackageNumber())
            .build();
    }

    /**
     * Converts DispatchPackageRepresentation to DispatchPackage with parent reference.
     */
    public DispatchPackage assemble(DispatchPackageRepresentation representation, DispatchBatch dispatchBatch) {
        return DispatchPackage.builder()
            .id(representation.getId())
            .dispatchBatch(dispatchBatch)
            .packagingType(representation.getPackagingType() != null 
                          ? PackagingType.valueOf(representation.getPackagingType())
                          : null)
            .quantityInPackage(representation.getQuantityInPackage())
            .packageNumber(representation.getPackageNumber())
            .build();
    }
} 