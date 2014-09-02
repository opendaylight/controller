package test.mock.util;

import java.util.Random;

public class Util {
    private static Random rnd = new Random();

    public static long nextLong(long RangeBottom, long rangeTop) {
        return RangeBottom + ((long)(rnd.nextDouble()*(rangeTop - RangeBottom)));
    }
}
