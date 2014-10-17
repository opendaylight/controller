package org.opendaylight.persisted;

import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;
import org.opendaylight.persisted.codec.ISerializer;

public class DataLocation implements ISerializer {

    private int startPosition = -1;
    private int length = -1;

    public DataLocation() {
    }

    public DataLocation(int _startPosition, int _length) {
        this.startPosition = _startPosition;
        this.length = _length;
    }

    public int getStartPosition() {
        return this.startPosition;
    }

    public int getLength() {
        return this.length;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {
        EncodeUtils.encodeInt32(this.startPosition, byteArray, location);
        EncodeUtils.encodeInt32(this.length, byteArray, location + 4);
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        encode(value, ba.getBytes(), ba.getLocation());
        ba.advance(8);
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return new DataLocation(EncodeUtils.decodeInt32(byteArray,
                location), EncodeUtils.decodeInt32(byteArray, location + 4));
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        Object result = decode(ba.getBytes(), ba.getLocation(), length);
        ba.advance(8);
        return result;
    }

    @Override
    public String getShardName(Object obj) {
        return null;
    }

    @Override
    public Object getRecordKey(Object obj) {
        return null;
    }

}
