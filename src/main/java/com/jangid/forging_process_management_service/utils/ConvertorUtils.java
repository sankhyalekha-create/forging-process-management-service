package com.jangid.forging_process_management_service.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConvertorUtils {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
}
