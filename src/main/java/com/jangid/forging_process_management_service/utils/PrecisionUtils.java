package com.jangid.forging_process_management_service.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for handling precision of quantity and weight measurements
 * in the forging process management system.
 * 
 * All weight/quantity fields use 4 decimal places precision which is standard
 * for manufacturing operations (allows for gram-level precision in KG measurements).
 */
public class PrecisionUtils {

  /**
   * Standard precision for all weight and quantity measurements
   * 4 decimal places = 0.0001 kg = 0.1 gram precision
   */
  public static final int QUANTITY_SCALE = 4;

  /**
   * Rounds a Double value to the standard quantity precision (4 decimal places)
   * 
   * @param value The value to round (can be null)
   * @return The rounded value, or null if input is null
   */
  public static Double roundQuantity(Double value) {
    if (value == null) {
      return null;
    }
    return BigDecimal.valueOf(value)
        .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP)
        .doubleValue();
  }

  /**
   * Rounds a double primitive to the standard quantity precision (4 decimal places)
   * 
   * @param value The value to round
   * @return The rounded value
   */
  public static double roundQuantity(double value) {
    return BigDecimal.valueOf(value)
        .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP)
        .doubleValue();
  }


  private PrecisionUtils() {
    // Utility class, prevent instantiation
  }
}
