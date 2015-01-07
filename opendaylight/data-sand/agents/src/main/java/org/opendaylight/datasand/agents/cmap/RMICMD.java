package org.opendaylight.datasand.agents.cmap;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;

public class RMICMD implements ISerializer {
    private long changeNumber = -1;
    private int operation = -1;
    private Object key = null;
    private Object value = null;

    protected RMICMD() {
    }

    public RMICMD(int _op, Object _key, Object _value, long _changeNumber) {
        this.operation = _op;
        this.key = _key;
        this.value = _value;
        this.changeNumber = _changeNumber;
    }

    public long getChangeNumber(){
        return this.changeNumber;
    }

    public int getOperation() {
        return operation;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {

    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        RMICMD c = (RMICMD) value;
        ba.getEncoder().encodeInt64(c.changeNumber, ba);
        ba.getEncoder().encodeInt32(c.operation, ba);
        if (c.key != null)
            ba.getEncoder().encodeObject(c.key, ba, c.key.getClass());
        else
            ba.getEncoder().encodeObject(c.key, ba, String.class);
        if (c.value != null)
            ba.getEncoder().encodeObject(c.value, ba, c.value.getClass());
        else
            ba.getEncoder().encodeObject(c.value, ba, String.class);
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        RMICMD c = new RMICMD();
        c.changeNumber = ba.getEncoder().decodeInt64(ba);
        c.operation = ba.getEncoder().decodeInt32(ba);
        c.key = ba.getEncoder().decodeObject(ba);
        c.value = ba.getEncoder().decodeObject(ba);
        return c;
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