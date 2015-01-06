package org.opendaylight.persisted.net;

import org.opendaylight.persisted.autoagents.ClassCodes;
import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;
import org.opendaylight.persisted.codec.ISerializer;

public class NetworkID implements ISerializer {
    private int[] address = null;
    static {
        EncodeUtils.registerSerializer(NetworkID.class, new NetworkID(),ClassCodes.NetworkID.getCode());
    }

    private NetworkID() {

    }

    public NetworkID(int _address, int _port, int _subSystemID) {
        this.address = new int[3];
        this.address[0] = _address;
        this.address[1] = _port;
        this.address[2] = _subSystemID;
    }

    public static NetworkID valueOf(String str) {
        String sdata[] = new String[4];
        String port = null;
        String subSystemID = null;

        int index = str.indexOf(".");
        sdata[0] = str.substring(0, index);
        int index1 = str.indexOf(".", index + 1);
        sdata[1] = str.substring(index + 1, index1);
        index = str.indexOf(".", index1 + 1);
        sdata[2] = str.substring(index1 + 1, index);
        index1 = str.indexOf(":");
        sdata[3] = str.substring(index + 1, index1);
        index = str.indexOf(":", index1 + 1);
        port = str.substring(index1 + 1, index);
        subSystemID = str.substring(index + 1);
        int addr = (Integer.parseInt(sdata[0]) * 16777216)
                + (Integer.parseInt(sdata[1]) * 65536)
                + (Integer.parseInt(sdata[2]) * 256)
                + (Integer.parseInt(sdata[3]));
        return new NetworkID(addr, Integer.parseInt(port),
                Integer.parseInt(subSystemID));
    }

    public int getPort() {
        return this.address[this.address.length - 2];
    }

    public int getSubSystemID() {
        return this.address[this.address.length - 1];
    }

    public int getIPv4Address() {
        return this.address[0];
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        // ipv4
        if (address.length == 3) {
            byte ipv4[] = new byte[4];
            EncodeUtils.encodeInt32(address[0], ipv4, 0);
            String ipString[] = new String[4];
            if (ipv4[0] < 0) {
                ipString[0] = "" + (256 + ipv4[0]);
            } else {
                ipString[0] = "" + ipv4[0];
            }
            if (ipv4[1] < 0) {
                ipString[1] = "" + (256 + ipv4[1]);
            } else {
                ipString[1] = "" + ipv4[1];
            }
            if (ipv4[2] < 0) {
                ipString[2] = "" + (256 + ipv4[2]);
            } else {
                ipString[2] = "" + ipv4[2];
            }
            if (ipv4[3] < 0) {
                ipString[3] = "" + (256 + ipv4[3]);
            } else {
                ipString[3] = "" + ipv4[3];
            }
            buff.append(ipString[0]).append(".").append(ipString[1])
                    .append(".").append(ipString[2]).append(".")
                    .append(ipString[3]).append(":")
                    .append(address[address.length - 2]).append(":")
                    .append(address[address.length - 1]);
            return buff.toString();
        } else
            return "Ipv6";
    }

    @Override
    public int hashCode() {
        return this.address[0] & this.address[this.address.length - 1];
    }

    @Override
    public boolean equals(Object obj) {
        NetworkID other = (NetworkID) obj;
        if (other.address.length == this.address.length) {
            for (int i = 0; i < this.address.length; i++) {
                if (this.address[i] != other.address[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int start) {
        NetworkID id = (NetworkID)value;
        for (int i = 0; i < id.address.length; i++) {
            if (i < id.address.length - 2)
                EncodeUtils.encodeInt32(id.address[i], byteArray, start);
            else if (i == id.address.length - 2)
                EncodeUtils.encodeInt16(id.address[i], byteArray, start + i * 4);
            else
                EncodeUtils.encodeInt16(id.address[i], byteArray, start + (i - 1) * 4 + 2);
        }
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        NetworkID id = (NetworkID)value;
        EncodeUtils.encodeInt32(id.getIPv4Address(), ba);
        EncodeUtils.encodeInt16(id.getPort(), ba);
        EncodeUtils.encodeInt16(id.getSubSystemID(), ba);
    }

    @Override
    public Object decode(byte[] byteArray, int start, int length) {
        int a = EncodeUtils.decodeInt32(byteArray, start);
        int b = EncodeUtils.decodeInt16(byteArray, start + 4);
        int c = EncodeUtils.decodeInt16(byteArray, start + 6);
        return new NetworkID(a, b, c);
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        int a = EncodeUtils.decodeInt32(ba);
        int b = EncodeUtils.decodeInt16(ba);
        int c = EncodeUtils.decodeInt16(ba);
        return new NetworkID(a, b, c);
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
