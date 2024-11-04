package com.jangid.forging_process_management_service.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConvertorUtils {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  public static LocalDateTime convertStringToLocalDateTime(String dateString){
    if(dateString == null || dateString.isEmpty()){
      return null;
    }
    return LocalDateTime.parse(dateString, formatter);
  }
}
