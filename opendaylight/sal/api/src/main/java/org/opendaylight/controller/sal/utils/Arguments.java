package org.opendaylight.controller.sal.utils;

public class Arguments {
    /**
     * Checks if supplied value is in range, otherwise throws
     * {@link IllegalArgumentException}.
     * 
     * @param min Minimal acceptable value
     * @param max Maximal acceptable value
     * @param value Value which is to be tested
     * @throws IllegalArgumentException If the value is not contained in supplied range.
     */
    public static void argInRange(int min, int max, int value)
            throws IllegalArgumentException {
        argInRange(min, max, value, "value");
    }
    
    
    /**
     * Checks if supplied value is in range, otherwise throws
     * {@link IllegalArgumentException}.
     * 
     * @param min Minimal acceptable value
     * @param max Maximal acceptable value
     * @param value Value which is to be tested
     * @throws IllegalArgumentException If the value is not contained in supplied range.
     */
    public static void argInRange(int min, int max, int value,String argName)
            throws IllegalArgumentException {
        if (value < min || max < value)
            throw new IllegalArgumentException(
                    "Supplied "+argName+" is not in range [0x"
                            + Integer.toHexString(min) + "-0x"
                            + Integer.toHexString(max) + "]");
    }
}
