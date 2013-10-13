package org.opendaylight.controller.sal.common.util;

public class Arguments {

    private Arguments() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Checks if value is instance of provided class
     * 
     * 
     * @param value Value to check
     * @param type Type to check
     * @return Reference which was checked
     */
    @SuppressWarnings("unchecked")
    public static <T> T checkInstanceOf(Object value, Class<T> type) {
        if(!type.isInstance(value))
            throw new IllegalArgumentException();
        return (T) value;
    }
}
