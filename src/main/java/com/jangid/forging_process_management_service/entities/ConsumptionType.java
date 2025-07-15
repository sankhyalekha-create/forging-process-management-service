package com.jangid.forging_process_management_service.entities;

/**
 * Represents the type of consumption for materials in the forging process.
 * This can be either quantity-based (weight/volume) or pieces-based.
 */
public enum ConsumptionType {
    /**
     * Quantity-based consumption, typically measured in weight (KG) or volume
     */
    QUANTITY,

    /**
     * Pieces-based consumption, counted in individual units
     */
    PIECES;

    /**
     * Validates if the given quantity value is valid for this consumption type
     * @param quantity The quantity value to validate
     * @param pieces The pieces value to validate
     * @return true if the values are valid for this consumption type
     */
    public boolean isValid(Double quantity, Integer pieces) {
        return switch (this) {
            case QUANTITY -> quantity != null && quantity > 0 && pieces == null;
            case PIECES -> pieces != null && pieces > 0 && quantity == null;
        };
    }
} 