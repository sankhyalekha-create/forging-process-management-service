package com.jangid.forging_process_management_service.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConvertorUtils {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter DATE_TIME_FORMATTER_ALT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static LocalDateTime convertStringToLocalDateTime(String dateString) {
    if (dateString == null || dateString.isEmpty()) {
      return null;
    }
    return LocalDateTime.parse(dateString, formatter);
  }

  // convertStringToLocalDate
  public static LocalDate convertStringToLocalDate(String dateString) {
    if (dateString == null || dateString.isEmpty()) {
      return null;
    }
    return LocalDate.parse(dateString, dateFormatter);
  }

  public static String convertLocalDateTimeToString(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.format(formatter);
  }

  /**
   * Parse date time from string with support for multiple formats
   * Supports: dd/MM/yyyy HH:mm:ss and yyyy-MM-dd HH:mm:ss
   */
  public static LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.isEmpty()) {
      return null;
    }
    
    try {
      // Try primary format: dd/MM/yyyy HH:mm:ss
      return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
    } catch (Exception e1) {
      try {
        // Try alternate format: yyyy-MM-dd HH:mm:ss
        return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER_ALT);
      } catch (Exception e2) {
        log.warn("Failed to parse date time: {} with both formats", dateTimeStr);
        return null;
      }
    }
  }
}
