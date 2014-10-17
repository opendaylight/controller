package org.opendaylight.persisted;

import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;

public class DataLocation {

    private int parentIndex = -1;
    private int recordIndex = -1;
    private int startPosition = -1;
    private int length = -1;

    public DataLocation() {
    }

    public DataLocation(int _startPosition, int _length,int _recordIndex,int _parentIndex) {
        this.startPosition = _startPosition;
        this.length = _length;
        this.recordIndex = _recordIndex;
        this.parentIndex = _parentIndex;
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

    public int getParentIndex(){
        return this.parentIndex;
    }

    public void updateLocationInfo(int _startPosition,int _length){
        this.startPosition = _startPosition;
        this.length = _length;
    }

    public void encode(byte[] byteArray, int location) {
        EncodeUtils.encodeInt32(this.startPosition, byteArray, location);
        EncodeUtils.encodeInt32(this.length, byteArray, location + 4);
        EncodeUtils.encodeInt32(this.recordIndex, byteArray,location+8);
        EncodeUtils.encodeInt32(this.parentIndex, byteArray,location+12);
    }

    public void encode(EncodeDataContainer ba) {
        ba.adjustSize(16);
        encode(ba.getBytes(), ba.getLocation());
        ba.advance(16);
    }

    public static DataLocation decode(byte[] byteArray, int location, int length) {
        return new DataLocation(EncodeUtils.decodeInt32(byteArray,location),
                                EncodeUtils.decodeInt32(byteArray, location + 4),
                                EncodeUtils.decodeInt32(byteArray, location + 8),
                                EncodeUtils.decodeInt32(byteArray, location + 12));
    }

    public static DataLocation decode(EncodeDataContainer ba, int length) {
        DataLocation result = decode(ba.getBytes(), ba.getLocation(), length);
        ba.advance(16);
        return result;
    }
}
