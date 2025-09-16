package com.jangid.forging_process_management_service.utils;

import java.security.SecureRandom;

public class AlphanumericGenerator {
//  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@!#$%^&*";
  private static final int LENGTH = 10;
  private static final SecureRandom RANDOM = new SecureRandom();

  public static String generateAlphanumericString() {
    StringBuilder sb = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      int index = RANDOM.nextInt(CHARACTERS.length());
      sb.append(CHARACTERS.charAt(index));
    }
    return sb.toString();
  }

  public static void main(String[] args) {
    String alphanumericString = generateAlphanumericString();
    System.out.println(alphanumericString);
  }
}
