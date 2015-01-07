package org.opendaylight.datasand.codec;

public class TypeDescriptorSerializer implements ISerializer{

    @Override
    public void encode(Object value, byte[] byteArray, int location) {
        // TODO Auto-generated method stub
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        TypeDescriptor ts = (TypeDescriptor)value;
        ts.encode(ba);
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        return TypeDescriptor.decode(ba);
    }

    @Override
    public String getShardName(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getRecordKey(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }
}
