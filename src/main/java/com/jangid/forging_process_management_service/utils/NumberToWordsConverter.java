package com.jangid.forging_process_management_service.utils;

import java.math.BigDecimal;

/**
 * Utility class to convert numbers to words in Indian English format.
 * Supports amounts up to 99,99,99,999.99 (Nine Crore Ninety Nine Lakh Ninety Nine Thousand Nine Hundred Ninety Nine and Ninety Nine Paise)
 */
public class NumberToWordsConverter {

  private static final String[] UNITS = {
      "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"
  };

  private static final String[] TEENS = {
      "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
      "Sixteen", "Seventeen", "Eighteen", "Nineteen"
  };

  private static final String[] TENS = {
      "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
  };

  /**
   * Converts a BigDecimal amount to words in Indian English format with "Rupees" and "Paise"
   * 
   * @param amount The amount to convert
   * @return The amount in words (e.g., "Seven Hundred Eight Rupees Only")
   */
  public static String convertToWords(BigDecimal amount) {
    if (amount == null) {
      return "Zero Rupees Only";
    }

    // Split into rupees and paise
    long rupees = amount.longValue();
    int paise = amount.subtract(new BigDecimal(rupees))
                      .multiply(new BigDecimal(100))
                      .intValue();

    StringBuilder words = new StringBuilder();

    if (rupees == 0 && paise == 0) {
      return "Zero Rupees Only";
    }

    // Convert rupees part
    if (rupees > 0) {
      words.append(convertNumberToWords(rupees));
      words.append(" Rupees");
    }

    // Convert paise part
    if (paise > 0) {
      if (rupees > 0) {
        words.append(" and ");
      }
      words.append(convertNumberToWords(paise));
      words.append(" Paise");
    }

    words.append(" Only");

    return words.toString().trim();
  }

  /**
   * Converts a long number to words in Indian numbering system
   * Supports: Crores, Lakhs, Thousands, Hundreds
   * 
   * @param number The number to convert
   * @return The number in words
   */
  private static String convertNumberToWords(long number) {
    if (number == 0) {
      return "Zero";
    }

    StringBuilder words = new StringBuilder();

    // Crores (10,000,000)
    if (number >= 10000000) {
      long crores = number / 10000000;
      words.append(convertNumberToWords(crores)).append(" Crore ");
      number %= 10000000;
    }

    // Lakhs (100,000)
    if (number >= 100000) {
      long lakhs = number / 100000;
      words.append(convertNumberToWords(lakhs)).append(" Lakh ");
      number %= 100000;
    }

    // Thousands (1,000)
    if (number >= 1000) {
      long thousands = number / 1000;
      words.append(convertNumberToWords(thousands)).append(" Thousand ");
      number %= 1000;
    }

    // Hundreds (100)
    if (number >= 100) {
      long hundreds = number / 100;
      words.append(UNITS[(int) hundreds]).append(" Hundred ");
      number %= 100;
    }

    // Tens and Units (1-99)
    if (number > 0) {
      if (number < 10) {
        words.append(UNITS[(int) number]);
      } else if (number < 20) {
        words.append(TEENS[(int) (number - 10)]);
      } else {
        int tensDigit = (int) (number / 10);
        int unitsDigit = (int) (number % 10);
        words.append(TENS[tensDigit]);
        if (unitsDigit > 0) {
          words.append(" ").append(UNITS[unitsDigit]);
        }
      }
    }

    return words.toString().trim();
  }

  /**
   * Main method for testing
   */
  public static void main(String[] args) {
    // Test cases
    System.out.println(convertToWords(new BigDecimal("0"))); // Zero Rupees Only
    System.out.println(convertToWords(new BigDecimal("1"))); // One Rupees Only
    System.out.println(convertToWords(new BigDecimal("10"))); // Ten Rupees Only
    System.out.println(convertToWords(new BigDecimal("25.50"))); // Twenty Five Rupees and Fifty Paise Only
    System.out.println(convertToWords(new BigDecimal("100"))); // One Hundred Rupees Only
    System.out.println(convertToWords(new BigDecimal("1000"))); // One Thousand Rupees Only
    System.out.println(convertToWords(new BigDecimal("100000"))); // One Lakh Rupees Only
    System.out.println(convertToWords(new BigDecimal("10000000"))); // One Crore Rupees Only
    System.out.println(convertToWords(new BigDecimal("708.00"))); // Seven Hundred Eight Rupees Only
    System.out.println(convertToWords(new BigDecimal("12345.67"))); // Twelve Thousand Three Hundred Forty Five Rupees and Sixty Seven Paise Only
    System.out.println(convertToWords(new BigDecimal("9999999.99"))); // Ninety Nine Lakh Ninety Nine Thousand Nine Hundred Ninety Nine Rupees and Ninety Nine Paise Only
  }
}

