package org.opendaylight.datasand.agents.cluster;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
import org.opendaylight.datasand.network.NetworkID;

public class CCommand implements ISerializer {
    private ChangeID changeID = null;
    private int operation = -1;
    private Object key = null;
    private Object value = null;

    protected CCommand() {
    }

    public CCommand(int _op, Object _key, Object _value,NetworkID netID) {
        this.operation = _op;
        this.key = _key;
        this.value = _value;
        this.changeID = new ChangeID(netID);
    }

    public ChangeID getChangeID(){
        return this.changeID;
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
        CCommand c = (CCommand) value;
        ba.getEncoder().encodeInt32(c.changeID.getAddress(), ba);
        ba.getEncoder().encodeInt32(c.changeID.getPort(), ba);
        ba.getEncoder().encodeInt32(c.changeID.getChangeNum(), ba);
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
        CCommand c = new CCommand();
        c.changeID = new ChangeID(ba.getEncoder().decodeInt32(ba),ba.getEncoder().decodeInt32(ba),ba.getEncoder().decodeInt32(ba));
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
