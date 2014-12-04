package test.mock.util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class TestUtils {

    private static AtomicLong transId = new AtomicLong();

    private static Random rnd = new Random();

    public static long nextLong(long RangeBottom, long rangeTop) {
        return RangeBottom + ((long)(rnd.nextDouble()*(rangeTop - RangeBottom)));
    }

    public static long getNewTransactionId() {
        return transId.incrementAndGet();
    }
}
