package org.opendaylight.datasand.codec;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class MD5Identifier {

    private long a = 0;
    private long b = 0;
    private static MessageDigest md = null;
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static WriteLock writeLock = lock.writeLock();

    private MD5Identifier(long[] l, int offset) {
        this.a = l[offset];
        this.b = l[offset + 1];
    }

    private MD5Identifier(long _a, long _b) {
        this.a = _a;
        this.b = _b;
    }

    private MD5Identifier(byte encodedRecordKey[]) {
        if (md == null) {
            try {
                writeLock.lock();
                if (md == null) {
                    try {
                        md = MessageDigest.getInstance("MD5");
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }

        byte by[] = null;

        try {
            writeLock.lock();
            md.update(encodedRecordKey);
            by = md.digest();
        } finally {
            writeLock.unlock();
        }

        a = (a << 8) + (by[0] & 0xff);
        a = (a << 8) + (by[1] & 0xff);
        a = (a << 8) + (by[2] & 0xff);
        a = (a << 8) + (by[3] & 0xff);
        a = (a << 8) + (by[4] & 0xff);
        a = (a << 8) + (by[5] & 0xff);
        a = (a << 8) + (by[6] & 0xff);
        a = (a << 8) + (by[7] & 0xff);

        b = (b << 8) + (by[8] & 0xff);
        b = (b << 8) + (by[9] & 0xff);
        b = (b << 8) + (by[10] & 0xff);
        b = (b << 8) + (by[11] & 0xff);
        b = (b << 8) + (by[12] & 0xff);
        b = (b << 8) + (by[13] & 0xff);
        b = (b << 8) + (by[14] & 0xff);
        b = (b << 8) + (by[15] & 0xff);

    }

    @Override
    public int hashCode() {
        return (int) (a ^ (a >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        MD5Identifier other = (MD5Identifier) obj;
        if (other.a == a && other.b == b)
            return true;
        return false;
    }

    public long getA() {
        return this.a;
    }

    public long getB() {
        return this.b;
    }

    public long[] toLongArray() {
        return new long[] { a, b };
    }

    public static MD5Identifier createX(String id) {
        return new MD5Identifier(id.getBytes());
    }

    public static MD5Identifier createX(byte data[]) {
        return new MD5Identifier(data);
    }

    public static MD5Identifier createX(long a, long b) {
        return new MD5Identifier(a, b);
    }

    public static MD5Identifier createX(long[] l) {
        return new MD5Identifier(l, 0);
    }

    public static MD5Identifier[] createXs(long[] l) {
        List<MD5Identifier> lst = new ArrayList<MD5Identifier>();
        for (int i = 0; i < l.length; i += 2) {
            lst.add(new MD5Identifier(l, i));
        }
        return lst.toArray(new MD5Identifier[lst.size()]);
    }

    public static long[] toLongArray(MD5Identifier X) {
        return X.toLongArray();
    }

    public static long[] toLongArray(MD5Identifier X[]) {
        long result[] = new long[X.length * 2];
        int i = 0;
        for (MD5Identifier c : X) {
            long la[] = c.toLongArray();
            System.arraycopy(la, 0, result, i, la.length);
            i += 2;
        }
        return result;
    }

    public static long[] toLongArray(List<MD5Identifier> lst) {
        long result[] = new long[lst.size() * 2];
        int i = 0;
        for (MD5Identifier c : lst) {
            long la[] = c.toLongArray();
            System.arraycopy(la, 0, result, i, la.length);
            i += 2;
        }
        return result;
    }
}