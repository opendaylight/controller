package org.opendaylight.persisted.codec;

public interface ISerializer {
    public void encode(Object value, byte[] byteArray, int location);

    public void encode(Object value, BytesArray ba);

    public Object decode(byte[] byteArray, int location, int length);

    public Object decode(BytesArray ba, int length);

    public String getBlockKey(Object obj);

    public Object getRecordKey(Object obj);
}
