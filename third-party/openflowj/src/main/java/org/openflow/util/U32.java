package org.openflow.util;

public class U32 {
    public static long f(int i) {
        return (long)i & 0xffffffffL;
    }

    public static int t(long l) {
        return (int) l;
    }
}
