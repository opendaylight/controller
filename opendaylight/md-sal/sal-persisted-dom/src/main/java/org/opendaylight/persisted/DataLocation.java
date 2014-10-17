package org.opendaylight.persisted;

import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;

public class DataLocation {

    private int recordIndex = -1;
    private int startPosition = -1;
    private int length = -1;

    public DataLocation() {
    }

    public DataLocation(int _startPosition, int _length,int _recordIndex) {
        this.startPosition = _startPosition;
        this.length = _length;
        this.recordIndex = _recordIndex;
    }

    public int getStartPosition() {
        return this.startPosition;
    }

    public int getLength() {
        return this.length;
    }

    public int getRecordIndex(){
        return this.recordIndex;
    }

    public void encode(byte[] byteArray, int location) {
        EncodeUtils.encodeInt32(this.startPosition, byteArray, location);
        EncodeUtils.encodeInt32(this.length, byteArray, location + 4);
        EncodeUtils.encodeInt32(this.recordIndex, byteArray,location+8);
    }

    public void encode(EncodeDataContainer ba) {
        ba.adjustSize(12);
        encode(ba.getBytes(), ba.getLocation());
        ba.advance(12);
    }

    public static DataLocation decode(byte[] byteArray, int location, int length) {
        return new DataLocation(EncodeUtils.decodeInt32(byteArray,location),
                                EncodeUtils.decodeInt32(byteArray, location + 4),
                                EncodeUtils.decodeInt32(byteArray, location + 8));
    }

    public static DataLocation decode(EncodeDataContainer ba, int length) {
        DataLocation result = decode(ba.getBytes(), ba.getLocation(), length);
        ba.advance(12);
        return result;
    }
}
