package org.opendaylight.datasand.codec;

public interface ISerializer {
    public void encode(Object value, byte[] byteArray, int location);

    public void encode(Object value, EncodeDataContainer ba);

    public Object decode(byte[] byteArray, int location, int length);

    public Object decode(EncodeDataContainer ba, int length);

    public String getShardName(Object obj);

    public Object getRecordKey(Object obj);
}
