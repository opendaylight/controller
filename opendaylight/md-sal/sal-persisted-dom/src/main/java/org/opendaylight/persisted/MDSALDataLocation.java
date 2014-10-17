package org.opendaylight.persisted;

import org.opendaylight.persisted.codec.BytesArray;
import org.opendaylight.persisted.codec.MDSALEncoder;
import org.opendaylight.persisted.codec.ISerializer;

public class MDSALDataLocation implements ISerializer {

    private int start = -1;
    private int size = -1;

    public MDSALDataLocation() {
    }

    public MDSALDataLocation(int _start, int _size) {
        this.start = _start;
        this.size = _size;
    }

    public int getStart() {
        return this.start;
    }

    public int getSize() {
        return this.size;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {
        MDSALEncoder.encodeInt32(this.start, byteArray, location);
        MDSALEncoder.encodeInt32(this.size, byteArray, location + 4);
    }

    @Override
    public void encode(Object value, BytesArray ba) {
        encode(value, ba.getBytes(), ba.getLocation());
        ba.advance(8);
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return new MDSALDataLocation(MDSALEncoder.decodeInt32(byteArray,
                location), MDSALEncoder.decodeInt32(byteArray, location + 4));
    }

    @Override
    public Object decode(BytesArray ba, int length) {
        Object result = decode(ba.getBytes(), ba.getLocation(), length);
        ba.advance(8);
        return result;
    }

    @Override
    public String getBlockKey(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getRecordKey(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }

}
