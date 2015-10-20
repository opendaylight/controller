package org.openflow.util;

import java.math.BigInteger;

public class U64 {
    public static BigInteger f(long i) {
        return new BigInteger(Long.toBinaryString(i), 2);
    }

    public static long t(BigInteger l) {
        return l.longValue();
    }
}
