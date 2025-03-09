package com.jangid.forging_process_management_service.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public final class GenericResourceUtils {

  private GenericResourceUtils() {
    throw new IllegalArgumentException();
  }

  public static Optional<Long> convertResourceIdToLong(String resourceId) {
    Long id = null;
    try {
      id = Long.parseLong(resourceId);
    } catch (NumberFormatException e) {
    }
    if (id == null) {
      log.warn("Invalid resourceId={}", resourceId);
      return Optional.empty();
    }
    return Optional.of(id);
  }

  public static Optional<Integer> convertResourceIdToInt(String resourceId) {
    Integer id = null;
    try {
      id = Integer.parseInt(resourceId);
    } catch (NumberFormatException e) {
      log.error("NumberFormatException during convertResourceIdToInt!");
    }
    if (id == null) {
      log.warn("Invalid resourceId={}", resourceId);
      return Optional.empty();
    }
    return Optional.of(id);
  }
}
