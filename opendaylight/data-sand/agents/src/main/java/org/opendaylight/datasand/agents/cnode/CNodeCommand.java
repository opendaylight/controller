package org.opendaylight.datasand.agents.cnode;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;

public class CNodeCommand<DataTypeElement> implements ISerializer {

    private long id = -1;
    private int operation = -1;
    private DataTypeElement data = null;

    protected CNodeCommand() {
    }

    public CNodeCommand(long _id,int _operation,DataTypeElement _data) {
        this.id = _id;
        this.operation = _operation;
        this.data = _data;
    }

    public long getID(){
        return this.id;
    }

    public int getOperation() {
        return operation;
    }

    public DataTypeElement getData(){
        return this.data;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        CNodeCommand c = (CNodeCommand) value;
        ba.getEncoder().encodeInt64(c.id, ba);
        ba.getEncoder().encodeInt32(c.operation, ba);
        ba.getEncoder().encodeObject(c.data, ba, null);
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        CNodeCommand<DataTypeElement> c = new CNodeCommand<DataTypeElement>();
        c.id = ba.getEncoder().decodeInt64(ba);
        c.operation = ba.getEncoder().decodeInt32(ba);
        c.data = (DataTypeElement)ba.getEncoder().decodeObject(ba);
        return c;
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