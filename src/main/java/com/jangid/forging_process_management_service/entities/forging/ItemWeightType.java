package com.jangid.forging_process_management_service.entities.forging;

/**
 * Enum representing the different types of item weights used in the forging process.
 */
public enum ItemWeightType {
    ITEM_WEIGHT,
    ITEM_SLUG_WEIGHT,
    ITEM_FORGED_WEIGHT,
    ITEM_FINISHED_WEIGHT;

    /**
     * Returns the default weight type, which is ITEM_WEIGHT.
     * @return The default ItemWeightType
     */
    public static ItemWeightType getDefault() {
        return ITEM_WEIGHT;
    }

    /**
     * Safely converts a string to ItemWeightType, returning the default if null or invalid.
     * @param value The string value to convert
     * @return The corresponding ItemWeightType or the default if the value is null or invalid
     */
    public static ItemWeightType fromString(String value) {
        if (value == null) {
            return getDefault();
        }
        
        try {
            return ItemWeightType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return getDefault();
        }
    }
} 