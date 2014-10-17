package org.opendaylight.persisted.net;

import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;
import org.opendaylight.persisted.codec.ISerializer;

public class Packet implements ISerializer {

    public static final int PACKET_SOURCE_LOCATION = 0;
    public static final int PACKET_SOURCE_LENGHT = 8;
    public static final int PACKET_DEST_LOCATION = PACKET_SOURCE_LOCATION
            + PACKET_SOURCE_LENGHT;
    public static final int PACKET_DEST_LENGTH = 8;
    public static final int PACKET_ID_LOCATION = PACKET_DEST_LOCATION
            + PACKET_DEST_LENGTH;
    public static final int PACKET_ID_LENGTH = 2;
    public static final int PACKET_MULTIPART_AND_PRIORITY_LOCATION = PACKET_ID_LOCATION
            + PACKET_ID_LENGTH;
    public static final int PACKET_MULTIPART_AND_PRIORITY_LENGTH = 1;
    public static final int PACKET_DATA_LOCATION = PACKET_MULTIPART_AND_PRIORITY_LOCATION
            + PACKET_MULTIPART_AND_PRIORITY_LENGTH;
    public static final int MAX_DATA_IN_ONE_PACKET = 1024 * 512;

    private NetworkID source = null;
    private NetworkID destination = null;
    private int packetID = -1;
    private boolean multiPart = false;
    private int priority = 2;
    private byte[] data = null;
    private Object decodedObject = null;

    private static int nextPacketID = 1000;
    static {
        EncodeUtils.registerSerializer(Packet.class, new Packet());
    }

    private Packet() {
    }

    public Packet(Object _decodedObject, NetworkID _source, NetworkID _dest) {
        this(_source, _dest, (byte[]) null);
        this.decodedObject = _decodedObject;
    }

    public Packet(NetworkID _source, NetworkID _destination, byte[] _data) {
        this(_source, _destination, _data, -1, false);
    }

    public Packet(NetworkID _source, NetworkID _destination, byte[] _data,
            int _id, boolean _multiPart) {
        if (_id == -1) {
            synchronized (Packet.class) {
                this.packetID = nextPacketID;
                nextPacketID++;
            }
        } else {
            this.packetID = _id;
        }
        this.multiPart = _multiPart;
        this.source = _source;
        this.destination = _destination;
        this.data = _data;
        if (this.data == null) {
            this.data = new byte[0];
        }
    }

    public NetworkID getSource() {
        return source;
    }

    public NetworkID getDestination() {
        return destination;
    }

    public int getPacketID() {
        return this.packetID;
    }

    public boolean isMultiPart() {
        return this.multiPart;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getPriority() {
        return this.priority;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {
        Packet p = (Packet) value;
        p.source.encode(p.source, byteArray, location + PACKET_SOURCE_LOCATION);
        p.destination.encode(p.destination, byteArray, location
                + PACKET_DEST_LOCATION);
        EncodeUtils.encodeInt16(p.packetID, byteArray, location
                + PACKET_ID_LOCATION);
        if (p.multiPart) {
            byte m_p = (byte) (p.priority * 2 + 1);
            byteArray[location + PACKET_MULTIPART_AND_PRIORITY_LOCATION] = m_p;
        } else {
            byte m_p = (byte) (p.priority * 2);
            byteArray[location + PACKET_MULTIPART_AND_PRIORITY_LOCATION] = m_p;
        }
        System.arraycopy(p.data, 0, byteArray, PACKET_DATA_LOCATION,
                p.data.length);
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        encode(value, ba);
        ba.advance(PACKET_DATA_LOCATION + data.length);
    }

    @Override
    public Object decode(byte[] byteArray, int start, int length) {
        Packet m = new Packet();
        m.source = (NetworkID) EncodeUtils.getSerializer(NetworkID.class)
                .decode(byteArray, start + PACKET_SOURCE_LOCATION, 8);
        m.destination = (NetworkID) EncodeUtils.getSerializer(NetworkID.class)
                .decode(byteArray, start + PACKET_DEST_LOCATION, 8);
        m.packetID = EncodeUtils.decodeInt16(byteArray, start
                + PACKET_ID_LOCATION);
        m.priority = ((int) byteArray[start
                + PACKET_MULTIPART_AND_PRIORITY_LOCATION]) / 2;
        m.multiPart = byteArray[start + PACKET_MULTIPART_AND_PRIORITY_LOCATION] % 2 == 1;
        m.data = new byte[length - PACKET_DATA_LOCATION];
        System.arraycopy(byteArray, start + PACKET_DATA_LOCATION, m.data, 0,
                length - PACKET_DATA_LOCATION);
        return m;
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        Object p = decode(ba.getBytes(), ba.getLocation(), length);
        ba.advance(length);
        return p;
    }

    @Override
    public int hashCode() {
        return source.getIPv4Address() ^ destination.getIPv4Address()
                ^ source.getPort() ^ destination.getPort()
                ^ source.getSubSystemID() ^ destination.getSubSystemID();
    }

    @Override
    public boolean equals(Object obj) {
        Packet other = (Packet) obj;
        if (source.equals(other.source)
                && destination.equals(other.destination)
                && packetID == other.packetID)
            return true;
        return false;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("ID=").append(this.packetID).append(" Source=")
                .append(this.source).append(" Dest=").append(this.destination);
        return buff.toString();
    }

    public void decode() {
        if (this.decodedObject == null) {
            EncodeDataContainer ba = new EncodeDataContainer(this.getData());
            try{decodedObject = EncodeUtils.decodeObject(ba);}catch(Exception err){
                System.err.println("Unable to decode...");
            }
        }
    }

    public Object getDecodedObject() {
        return this.decodedObject;
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
