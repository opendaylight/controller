package org.opendaylight.datasand.agents.cluster;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
import org.opendaylight.datasand.network.NetworkID;

public class ChangeID implements ISerializer{

    private static int nextID = 1000;
    private int address = -1;
    private int port = -1;
    private int changeNum = -1;

    protected ChangeID(){
    }

    public ChangeID(NetworkID netID){
        synchronized (ChangeID.class) {
            nextID++;
            this.changeNum = nextID;
        }
        this.address = netID.getIPv4Address();
        this.port = netID.getPort();
    }

    public ChangeID(int _addr,int _port,int _changenum){
        this.address = _addr;
        this.port = _port;
        this.changeNum = _changenum;
    }

    public int getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getChangeNum() {
        return changeNum;
    }

    @Override
    public boolean equals(Object obj) {
        ChangeID other = (ChangeID)obj;
        if(this.address==other.address && this.port == other.port && this.changeNum==other.changeNum)
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return address+port+changeNum;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {
        // TODO Auto-generated method stub
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        ChangeID cid = (ChangeID)value;
        ba.getEncoder().encodeInt32(cid.address, ba);
        ba.getEncoder().encodeInt32(cid.port, ba);
        ba.getEncoder().encodeInt32(cid.changeNum, ba);
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        ChangeID cid = new ChangeID();
        cid.address = ba.getEncoder().decodeInt32(ba);
        cid.port = ba.getEncoder().decodeInt32(ba);
        cid.changeNum = ba.getEncoder().decodeInt32(ba);
        return cid;
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