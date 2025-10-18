package com.jangid.forging_process_management_service.assemblers.gst;

import com.jangid.forging_process_management_service.entities.gst.GSTConfiguration;
import com.jangid.forging_process_management_service.entitiesRepresentation.gst.GSTConfigurationRepresentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GSTConfigurationAssembler {

    /**
     * Convert GSTConfigurationRepresentation to GSTConfiguration entity
     */
    public GSTConfiguration assemble(GSTConfigurationRepresentation representation) {
        if (representation == null) {
            return null;
        }

        try {
            return GSTConfiguration.builder()
                .id(representation.getId())
                .companyGstin(representation.getCompanyGstin())
                .companyLegalName(representation.getCompanyLegalName())
                .companyTradeName(representation.getCompanyTradeName())
                .companyAddress(representation.getCompanyAddress())
                .companyStateCode(representation.getCompanyStateCode())
                .companyPincode(representation.getCompanyPincode())
                .invoiceNumberPrefix(representation.getInvoiceNumberPrefix())
                .currentInvoiceSequence(representation.getCurrentInvoiceSequence())
                .challanNumberPrefix(representation.getChallanNumberPrefix())
                .currentChallanSequence(representation.getCurrentChallanSequence())
                .ewayBillThreshold(representation.getEwayBillThreshold())
                .autoGenerateEwayBill(representation.getAutoGenerateEwayBill())
                .defaultCgstRate(representation.getDefaultCgstRate())
                .defaultSgstRate(representation.getDefaultSgstRate())
                .defaultIgstRate(representation.getDefaultIgstRate())
                .isActive(representation.getIsActive())
                .build();
        } catch (Exception e) {
            log.error("Error assembling GSTConfiguration from representation", e);
            throw new RuntimeException("Failed to assemble GSTConfiguration: " + e.getMessage(), e);
        }
    }

    /**
     * Convert GSTConfiguration entity to GSTConfigurationRepresentation
     */
    public GSTConfigurationRepresentation disassemble(GSTConfiguration entity) {
        if (entity == null) {
            return null;
        }

        try {
            return GSTConfigurationRepresentation.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .companyGstin(entity.getCompanyGstin())
                .companyLegalName(entity.getCompanyLegalName())
                .companyTradeName(entity.getCompanyTradeName())
                .companyAddress(entity.getCompanyAddress())
                .companyStateCode(entity.getCompanyStateCode())
                .companyPincode(entity.getCompanyPincode())
                .invoiceNumberPrefix(entity.getInvoiceNumberPrefix())
                .currentInvoiceSequence(entity.getCurrentInvoiceSequence())
                .challanNumberPrefix(entity.getChallanNumberPrefix())
                .currentChallanSequence(entity.getCurrentChallanSequence())
                .ewayBillThreshold(entity.getEwayBillThreshold())
                .autoGenerateEwayBill(entity.getAutoGenerateEwayBill())
                .defaultCgstRate(entity.getDefaultCgstRate())
                .defaultSgstRate(entity.getDefaultSgstRate())
                .defaultIgstRate(entity.getDefaultIgstRate())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        } catch (Exception e) {
            log.error("Error disassembling GSTConfiguration to representation", e);
            throw new RuntimeException("Failed to disassemble GSTConfiguration: " + e.getMessage(), e);
        }
    }

    /**
     * Convert list of GSTConfiguration entities to list of representations
     */
    public List<GSTConfigurationRepresentation> disassemble(List<GSTConfiguration> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
            .map(this::disassemble)
            .collect(Collectors.toList());
    }

    /**
     * Update existing GSTConfiguration entity with data from representation
     */
    public GSTConfiguration updateEntity(GSTConfiguration existingEntity, GSTConfigurationRepresentation representation) {
        if (existingEntity == null || representation == null) {
            return existingEntity;
        }

        try {
            // Update only non-null fields from representation
            if (representation.getCompanyGstin() != null) {
                existingEntity.setCompanyGstin(representation.getCompanyGstin());
            }
            if (representation.getCompanyLegalName() != null) {
                existingEntity.setCompanyLegalName(representation.getCompanyLegalName());
            }
            if (representation.getCompanyTradeName() != null) {
                existingEntity.setCompanyTradeName(representation.getCompanyTradeName());
            }
            if (representation.getCompanyAddress() != null) {
                existingEntity.setCompanyAddress(representation.getCompanyAddress());
            }
            if (representation.getCompanyStateCode() != null) {
                existingEntity.setCompanyStateCode(representation.getCompanyStateCode());
            }
            if (representation.getCompanyPincode() != null) {
                existingEntity.setCompanyPincode(representation.getCompanyPincode());
            }
            if (representation.getInvoiceNumberPrefix() != null) {
                existingEntity.setInvoiceNumberPrefix(representation.getInvoiceNumberPrefix());
            }
            if (representation.getCurrentInvoiceSequence() != null) {
                existingEntity.setCurrentInvoiceSequence(representation.getCurrentInvoiceSequence());
            }
            if (representation.getChallanNumberPrefix() != null) {
                existingEntity.setChallanNumberPrefix(representation.getChallanNumberPrefix());
            }
            if (representation.getCurrentChallanSequence() != null) {
                existingEntity.setCurrentChallanSequence(representation.getCurrentChallanSequence());
            }
            if (representation.getEwayBillThreshold() != null) {
                existingEntity.setEwayBillThreshold(representation.getEwayBillThreshold());
            }
            if (representation.getAutoGenerateEwayBill() != null) {
                existingEntity.setAutoGenerateEwayBill(representation.getAutoGenerateEwayBill());
            }
            if (representation.getDefaultCgstRate() != null) {
                existingEntity.setDefaultCgstRate(representation.getDefaultCgstRate());
            }
            if (representation.getDefaultSgstRate() != null) {
                existingEntity.setDefaultSgstRate(representation.getDefaultSgstRate());
            }
            if (representation.getDefaultIgstRate() != null) {
                existingEntity.setDefaultIgstRate(representation.getDefaultIgstRate());
            }
            if (representation.getIsActive() != null) {
                existingEntity.setIsActive(representation.getIsActive());
            }

            return existingEntity;
        } catch (Exception e) {
            log.error("Error updating GSTConfiguration entity from representation", e);
            throw new RuntimeException("Failed to update GSTConfiguration: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new GSTConfiguration entity for tenant with default values
     */
    public GSTConfiguration createDefaultConfiguration(Long tenantId, String companyGstin, String companyName) {
        try {
            return GSTConfiguration.builder()
                .companyGstin(companyGstin)
                .companyLegalName(companyName)
                .companyStateCode(extractStateCodeFromGstin(companyGstin))
                .invoiceNumberPrefix("INV")
                .currentInvoiceSequence(1)
                .challanNumberPrefix("CHN")
                .currentChallanSequence(1)
                .isActive(true)
                .build();
        } catch (Exception e) {
            log.error("Error creating default GSTConfiguration for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to create default GSTConfiguration: " + e.getMessage(), e);
        }
    }

    // Private helper methods
    private String extractStateCodeFromGstin(String gstin) {
        if (gstin != null && gstin.length() >= 2) {
            return gstin.substring(0, 2);
        }
        return null;
    }
}
