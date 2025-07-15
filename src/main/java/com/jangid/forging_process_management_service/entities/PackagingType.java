package com.jangid.forging_process_management_service.entities;

/**
 * Enum representing the different types of packaging used across the system.
 */
public enum PackagingType {
    Box,
    Bag,
    Jaal,
    Jaali,
    Loose;

    /**
     * Returns the default packaging type, which is Box.
     * @return The default PackagingType
     */
    public static PackagingType getDefault() {
        return Box;
    }

    /**
     * Safely converts a string to PackagingType, returning the default if null or invalid.
     * @param value The string value to convert
     * @return The corresponding PackagingType or the default if the value is null or invalid
     */
    public static PackagingType fromString(String value) {
        if (value == null) {
            return getDefault();
        }
        
        try {
            return PackagingType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return getDefault();
        }
    }
} 