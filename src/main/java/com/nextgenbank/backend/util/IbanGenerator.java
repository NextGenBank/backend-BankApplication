package com.nextgenbank.backend.util;

import java.util.Random;

public class IbanGenerator {
    private static final String COUNTRY_CODE = "NL";
    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final Random random = new Random();
    private static int counter = 1; // Simple counter to ensure uniqueness

    /**
     * Generate a random IBAN for the Netherlands
     * @return A random IBAN
     */
    public static String generateIban() {
        StringBuilder sb = new StringBuilder(COUNTRY_CODE);
        
        // Add two random check digits
        sb.append(String.format("%02d", random.nextInt(100)));
        
        // Add bank code (e.g., ABNA for ABN AMRO)
        sb.append("BANK");
        
        // Add account number with counter to ensure uniqueness
        String uniqueNumber = String.format("%010d", counter++);
        sb.append(uniqueNumber);
        
        return sb.toString();
    }
}