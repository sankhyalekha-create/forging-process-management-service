/*
 *
 *    ADOBE CONFIDENTIAL
 *    ___________________
 *
 *    Copyright 2018 Adobe Systems Incorporated
 *    All Rights Reserved.
 *
 *    NOTICE:  All information contained herein is, and remains
 *    the property of Adobe Systems Incorporated and its suppliers,
 *    if any.  The intellectual and technical concepts contained
 *    herein are proprietary to Adobe Systems Incorporated and its
 *    suppliers and are protected by all applicable intellectual property
 *    laws, including trade secret and copyright laws.
 *    Dissemination of this information or reproduction of this material
 *    is strictly forbidden unless prior written permission is obtained
 *    from Adobe Systems Incorporated.
 *
 */

package com.jangid.forging_process_management_service.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Contains various utility functions to be used inside Resource files
 */
@Slf4j
public final class ResourceUtils {

  private ResourceUtils() {
    throw new IllegalArgumentException();
  }

  public static Optional<Long> convertIdToLong(String resourceId) {
    Long id = null;
    try {
      id = Long.parseLong(resourceId);
    } catch (NumberFormatException e) {
      // ignore this exception as id will remain null
    }
    if (id == null) {
      log.warn("Invalid resourceId={}", resourceId);
      return Optional.empty();
    }
    return Optional.of(id);
  }

  public static Long toLongOrNull(String resourceId) {
    Optional<Long> id = convertIdToLong(resourceId);
    return id.orElse(null);
  }
}
