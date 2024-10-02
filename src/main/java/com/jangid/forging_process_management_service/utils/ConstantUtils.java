package com.jangid.forging_process_management_service.utils;

import java.time.format.DateTimeFormatter;

public class ConstantUtils {
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  public static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public static final String LAST_MINUTE_OF_DAY = "T23:59";


}
