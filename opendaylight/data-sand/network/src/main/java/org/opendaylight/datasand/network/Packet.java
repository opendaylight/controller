package org.opendaylight.datasand.network;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;


public class Packet implements ISerializer {

    public static final int PACKET_SOURCE_LOCATION = 0;
    public static final int PACKET_SOURCE_LENGHT = 8;
    public static final int PACKET_DEST_LOCATION = PACKET_SOURCE_LOCATION + PACKET_SOURCE_LENGHT;
    public static final int PACKET_DEST_LENGTH = 8;
    public static final int PACKET_ID_LOCATION = PACKET_DEST_LOCATION + PACKET_DEST_LENGTH;
    public static final int PACKET_ID_LENGTH = 2;
    public static final int PACKET_MULTIPART_AND_PRIORITY_LOCATION = PACKET_ID_LOCATION + PACKET_ID_LENGTH;
    public static final int PACKET_MULTIPART_AND_PRIORITY_LENGTH = 1;
    public static final int PACKET_DATA_LOCATION = PACKET_MULTIPART_AND_PRIORITY_LOCATION + PACKET_MULTIPART_AND_PRIORITY_LENGTH;
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
        ByteEncoder.registerSerializer(Packet.class, new Packet(),NetworkClassCodes.CODE_Packet);
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
        p.destination.encode(p.destination, byteArray, location+ PACKET_DEST_LOCATION);
        ByteEncoder.encodeInt16(p.packetID, byteArray, location+ PACKET_ID_LOCATION);
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
    public void encode(Object value, EncodeDataContainer _ba) {
        ByteArrayEncodeDataContainer ba = (ByteArrayEncodeDataContainer)_ba;
        encode(value, ba);
        ba.advance(PACKET_DATA_LOCATION + data.length);
    }

    @Override
    public Object decode(byte[] byteArray, int start, int length) {
        Packet m = new Packet();
        m.source = (NetworkID) ByteEncoder.getSerializer(NetworkID.class,null)
                .decode(byteArray, start + PACKET_SOURCE_LOCATION, 8);
        m.destination = (NetworkID) ByteEncoder.getSerializer(NetworkID.class,null)
                .decode(byteArray, start + PACKET_DEST_LOCATION, 8);
        m.packetID = ByteEncoder.decodeInt16(byteArray, start
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
    public Object decode(EncodeDataContainer _ba, int length) {
        ByteArrayEncodeDataContainer ba = (ByteArrayEncodeDataContainer)_ba;
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

    public void decode(TypeDescriptorsContainer container) {
        if (this.decodedObject == null) {
            if(this.getSource().equals(NetworkNodeConnection.PROTOCOL_ID_UNREACHABLE)){
                byte origData[] = new byte[this.getData().length-PACKET_DATA_LOCATION];
                System.arraycopy(this.getData(), PACKET_DATA_LOCATION, origData, 0, origData.length);
                ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(origData,container);
                try{
                    decodedObject = ba.getEncoder().decodeObject(ba);
                }catch(Exception err){
                    System.err.println("Unable to decode...");
                }
            }else{
                ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(this.getData(),container);
                try{
                    decodedObject = ba.getEncoder().decodeObject(ba);}
                catch(Exception err){
                    System.err.println("Unable to decode...");
                }
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

    public NetworkID getUnreachableOrigAddress(){
        int destAddress = ByteEncoder.decodeInt32(this.getData(),PACKET_DEST_LOCATION);
        int destPort = ByteEncoder.decodeInt16(this.getData(), PACKET_DEST_LOCATION+4);
        if(destAddress==0){
            destAddress = ByteEncoder.decodeInt32(this.getData(), this.getData().length-6);
            destPort = ByteEncoder.decodeInt16(this.getData(), this.getData().length-2);
        }
        return new NetworkID(destAddress, destPort, 0);
    }
}
