package org.openflow.util;

public class U16 {
    public static int f(short i) {
        return (int)i & 0xffff;
    }

    public static short t(int l) {
        return (short) l;
    }
}
