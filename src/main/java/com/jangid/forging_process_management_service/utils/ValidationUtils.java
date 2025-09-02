package com.jangid.forging_process_management_service.utils;

public class ValidationUtils {

    private static final String PHONE_NUMBER_REGEX = "^(\\+?91[-\\s]?[6-9]\\d{9}|[6-9]\\d{9}|0\\d{2,4}-?\\d{6,8})$";
    private static final String PAN_NUMBER_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$";
    private static final String GSTIN_REGEX = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";

    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return phoneNumber.matches(PHONE_NUMBER_REGEX);
    }

    public static boolean isValidPanNumber(String panNumber) {
        if (panNumber == null || panNumber.trim().isEmpty()) {
            return false;
        }
        return panNumber.matches(PAN_NUMBER_REGEX);
    }

    public static boolean isValidGstinNumber(String gstinNumber) {
        if (gstinNumber == null || gstinNumber.trim().isEmpty()) {
            return false;
        }
        return gstinNumber.matches(GSTIN_REGEX);
    }
} 