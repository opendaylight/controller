package org.opendaylight.persisted.net;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.persisted.codec.EncodeUtils;

public class NetworkNode extends Thread {

    private ServerSocket socket = null;
    private NetworkID localHost = null;
    private boolean running = false;

    private NetworkNodeConnection connection = null;
    private Map<Integer, Map<Integer, NetworkNodeConnection>> routingTable = new ConcurrentHashMap<Integer, Map<Integer, NetworkNodeConnection>>();

    protected Set<NetworkID> incomingConnections = new HashSet<NetworkID>();

    private PacketProcessor packetProcessor = null;
    private IFrameListener frameListener = null;
    private Packet serializer = new Packet((Object) null, null, null);

    public NetworkNode(IFrameListener _frameListener) {
        this.frameListener = _frameListener;
        for (int i = 50000; i < 60000; i++) {
            try {
                new NetworkID(0, 0, 0);
                new Packet(localHost, localHost, (byte[]) null);
                int localhost = NetworkID.valueOf(
                        EncodeUtils.getLocalIPAddress() + ":0:0")
                        .getIPv4Address();
                socket = new ServerSocket(i);
                localHost = new NetworkID(localhost, i, 0);
                if (i > 50000)
                    new RequestConnection();
                this.setName("Node-" + this.getLocalHost());
                this.start();
            } catch (Exception err) {
            }
            if (socket != null)
                break;
        }
        //Sleep for 100 to allow the threads to start up
        try{Thread.sleep(100);}catch(Exception err){}
    }

    public void shutdown() {
        this.running = false;
        try {
            this.connection.shutdown();
        } catch (Exception err) {
        }
        try {
            this.socket.close();
        } catch (Exception err) {
        }
        this.socket = null;
        for (Map<Integer, NetworkNodeConnection> map : routingTable.values()) {
            for (NetworkNodeConnection con : map.values()) {
                try {
                    con.shutdown();
                } catch (Exception err) {
                }
            }
        }
    }

    public void run() {
        running = true;
        packetProcessor = new PacketProcessor();
        try {
            while (running) {
                Socket s = socket.accept();
                new NetworkNodeConnection(this, s);
            }
        } catch (Exception err) {
            // err.printStackTrace();
        }
        System.out.println(this.getName()+" was shutdown.");
    }

    public void send(byte data[], NetworkID source, NetworkID dest) {
        if (data.length < Packet.MAX_DATA_IN_ONE_PACKET) {
            Packet p = new Packet(source, dest, data);
            send(p);
        } else {
            int count = data.length / Packet.MAX_DATA_IN_ONE_PACKET;
            if (data.length % Packet.MAX_DATA_IN_ONE_PACKET > 0)
                count++;
            byte[] countData = new byte[4];
            EncodeUtils.encodeInt32(count, countData, 0);
            Packet header = new Packet(source, dest, countData, -1, true);
            send(header);
            for (int i = 0; i < count; i++) {
                byte[] pData = new byte[Packet.MAX_DATA_IN_ONE_PACKET];
                if (i < count - 1) {
                    System.arraycopy(data, i * Packet.MAX_DATA_IN_ONE_PACKET,
                            pData, 0, pData.length);
                } else {
                    System.arraycopy(data, i * Packet.MAX_DATA_IN_ONE_PACKET,
                            pData, 0, data.length
                                    - (i * Packet.MAX_DATA_IN_ONE_PACKET));
                }
                send(new Packet(source, dest, pData, header.getPacketID(), true));
            }
        }
    }

    public void send(Packet m) {
        if (this.connection != null) {
            try {
                byte pData[] = new byte[Packet.PACKET_DATA_LOCATION + m.getData().length];
                m.encode(m, pData, 0);
                this.connection.sendPacket(pData);
            } catch (Exception err) {
                err.printStackTrace();
            }
        } else {
            //Multicast/Broadcast from the switch
            if(m.getDestination().getIPv4Address()==0){
                NetworkNodeConnection sourceCon = getNodeConnection(m.getSource().getIPv4Address(), m.getSource().getPort());
                for(Map.Entry<Integer,Map<Integer, NetworkNodeConnection>> entry:this.routingTable.entrySet()){
                    int destAddress = entry.getKey();
                    Map<Integer,NetworkNodeConnection> map = entry.getValue();
                    for(Map.Entry<Integer, NetworkNodeConnection> pMap:map.entrySet()){
                        int destPort = pMap.getKey();
                        NetworkNodeConnection c = pMap.getValue();
                        try {
                            byte pData[] = new byte[Packet.PACKET_DATA_LOCATION+ m.getData().length];
                            m.encode(m, pData, 0);
                            byte[] unreachable = c.sendPacket(pData);
                            if(unreachable!=null){
                                unreachable = NetworkNodeConnection.addUnreachableAddressForMulticast(unreachable, destAddress, destPort);
                                if(sourceCon!=null){
                                    sourceCon.sendPacket(unreachable);
                                }else{
                                    this.receivedPacket(unreachable);
                                }
                            }
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                }
            }else{
                NetworkNodeConnection c = this.getNodeConnection(m.getDestination().getIPv4Address(), m.getDestination().getPort());
                if (c != null) {
                    try {
                        byte pData[] = new byte[Packet.PACKET_DATA_LOCATION+ m.getData().length];
                        m.encode(m, pData, 0);
                        byte unreachable[] = c.sendPacket(pData);
                        if(unreachable!=null){
                            unregisterNetworkNodeConnection(m.getDestination());
                            this.receivedPacket(unreachable);
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                } else {
                    byte pData[] = new byte[Packet.PACKET_DATA_LOCATION+ m.getData().length];
                    m.encode(m, pData, 0);
                    byte data[] = NetworkNodeConnection.markAsUnreachable(pData);
                    this.receivedPacket(data);
                }
            }
        }
    }

    private class RequestConnection extends Thread {

        public RequestConnection() {
            this.setName("Request Connection Thread R:"
                    + getLocalHost().getPort() + "    ");
            this.start();
        }

        public void run() {
            while (connection == null) {
                String conString = "R:" + getLocalHost().getPort() + "    ";
                try {
                    Socket s = new Socket("localhost", 50000);
                    s.getOutputStream().write(conString.getBytes());
                    s.getOutputStream().flush();
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(1000);
                        if (connection != null)
                            break;
                    }
                    s.close();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

    public NetworkID getLocalHost() {
        return this.localHost;
    }

    public boolean registerNetworkNodeConnection(NetworkNodeConnection c,NetworkID source) {
        synchronized (this) {
            c.setName("Con " + getLocalHost() + "<->" + source);
            if (this.localHost.getPort() != 50000 && this.connection == null) {
                this.connection = c;
                return true;
            } else {
                Map<Integer, NetworkNodeConnection> map = this.routingTable.get(source.getIPv4Address());
                if (map == null) {
                    map = new ConcurrentHashMap<Integer, NetworkNodeConnection>();
                    this.routingTable.put(source.getIPv4Address(), map);
                }
                if (map.containsKey(source.getPort())) {
                    return false;
                }
                map.put(source.getPort(), c);
                return true;
            }
        }
    }

    public void unregisterNetworkNodeConnection(NetworkID source){
        synchronized (this) {
            System.out.println("Unregister "+source);
            Map<Integer, NetworkNodeConnection> map = this.routingTable.get(source.getIPv4Address());
            if(map!=null){
                map.remove(source.getPort());
            }
        }
    }

    public void broadcast(byte data[]) {
        int sourceAddress = EncodeUtils.decodeInt32(data,Packet.PACKET_SOURCE_LOCATION);
        int sourcePort = EncodeUtils.decodeInt16(data,Packet.PACKET_SOURCE_LOCATION+4);
        NetworkNodeConnection sourceCon = getNodeConnection(sourceAddress, sourcePort);
        List<NetworkID> unreachableDest = new LinkedList<NetworkID>();
        for (Map.Entry<Integer, Map<Integer, NetworkNodeConnection>> addrEntry : this.routingTable.entrySet()) {
            for (Map.Entry<Integer, NetworkNodeConnection> portEntry : addrEntry.getValue().entrySet()) {
                byte unreachable[] = null;
                if (sourceAddress != this.getLocalHost().getIPv4Address()) {
                    if (addrEntry.getKey() == this.getLocalHost().getIPv4Address()) {
                        try {
                            unreachable = portEntry.getValue().sendPacket(data);
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                } else {
                    try {
                        unreachable = portEntry.getValue().sendPacket(data);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
                if(unreachable!=null){
                    NetworkID nid = new NetworkID(addrEntry.getKey(),portEntry.getKey(), 0);
                    unreachableDest.add(nid);
                    unreachable = NetworkNodeConnection.addUnreachableAddressForMulticast(unreachable, addrEntry.getKey(), portEntry.getKey());
                    if(sourceCon!=null){
                        try{
                            sourceCon.sendPacket(unreachable);
                        }catch(Exception err){
                            err.printStackTrace();
                        }
                    }else{
                        this.receivedPacket(unreachable);
                    }
                }
            }
        }
        if(!unreachableDest.isEmpty()){
            for(NetworkID unreach:unreachableDest){
                unregisterNetworkNodeConnection(unreach);
            }
        }
        this.receivedPacket(data);
    }

    public void receivedPacket(byte data[]) {
        packetProcessor.addPacket(data);
    }

    public NetworkNodeConnection getNodeConnection(int address, int port) {
        Map<Integer, NetworkNodeConnection> map = routingTable.get(address);
        if (map == null)
            return null;
        return map.get(port);
    }

    public void joinNetwork(String host) {
        if (this.getLocalHost().getPort() != 50000) {
            System.err
                    .println("Only the node binded to port 50000 can join an external network.");
            return;
        }
        try {
            new NetworkNodeConnection(this, InetAddress.getByName(host), 50000);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private class PacketProcessor extends Thread {
        private LinkedList<byte[]> incomingFrames = new LinkedList<byte[]>();
        private Map<Packet, MultiPartContainer> multiparts = new HashMap<Packet, MultiPartContainer>();

        private class MultiPartContainer {
            private List<byte[]> parts = new LinkedList<byte[]>();
            private int expectedCount = -1;

            public byte[] toFrame() {
                byte[] firstPart = parts.get(0);
                byte[] data = new byte[(firstPart.length - Packet.PACKET_DATA_LOCATION)
                        * parts.size() + Packet.PACKET_DATA_LOCATION];
                System.arraycopy(firstPart, 0, data, 0,
                        Packet.PACKET_DATA_LOCATION);
                int location = Packet.PACKET_DATA_LOCATION;
                for (byte[] p : parts) {
                    System.arraycopy(p, Packet.PACKET_DATA_LOCATION, data,
                            location, p.length - Packet.PACKET_DATA_LOCATION);
                    location += (p.length - Packet.PACKET_DATA_LOCATION);
                }
                return data;
            }
        }

        public PacketProcessor() {
            this.setName("Packet Processor For " + getLocalHost());
            this.start();
        }

        public void addPacket(byte[] packet) {
            boolean multiPart = packet[Packet.PACKET_MULTIPART_AND_PRIORITY_LOCATION] % 2 == 1;

            // The packet is a complete frame
            if (!multiPart) {
                synchronized (incomingFrames) {
                    incomingFrames.add(packet);
                    incomingFrames.notifyAll();
                }
            } else {
                Packet pID = (Packet) serializer.decode(packet, 0,
                        Packet.PACKET_DATA_LOCATION);
                MultiPartContainer mpc = multiparts.get(pID);
                if (mpc == null) {
                    mpc = new MultiPartContainer();
                    multiparts.put(pID, mpc);
                    mpc.expectedCount = EncodeUtils.decodeInt32(packet,
                            Packet.PACKET_DATA_LOCATION);
                } else {
                    mpc.parts.add(packet);
                    if (mpc.parts.size() == mpc.expectedCount) {
                        multiparts.remove(pID);
                        byte frame[] = mpc.toFrame();
                        synchronized (incomingFrames) {
                            incomingFrames.add(frame);
                            incomingFrames.notifyAll();
                        }
                    }
                }
            }
        }

        public void run() {
            while (running) {
                byte frame[] = null;
                synchronized (incomingFrames) {
                    if (incomingFrames.size() == 0) {
                        try {
                            incomingFrames.wait(5000);
                        } catch (Exception err) {
                        }
                    }
                    if (incomingFrames.size() > 0) {
                        frame = incomingFrames.removeFirst();
                    }
                }

                if (frame != null) {
                    Packet f = (Packet) serializer.decode(frame, 0,frame.length);
                    if (frameListener != null) {
                        if (f.getSource().getIPv4Address() == 0 && f.getSource().getSubSystemID() == 9999) {
                            frameListener.processDestinationUnreachable(f);
                        } else if (f.getDestination().getIPv4Address() == 0
                                && f.getDestination().getSubSystemID() == NetworkNodeConnection.DESTINATION_BROADCAST) {
                            frameListener.processBroadcast(f);
                        } else if (f.getDestination().getIPv4Address() == 0
                                && f.getDestination().getSubSystemID() > NetworkNodeConnection.DESTINATION_BROADCAST) {
                            frameListener.processMulticast(f);
                        } else
                            frameListener.process(f);
                    } else {
                        if (f.getSource().getIPv4Address() == 0
                                && f.getSource().getSubSystemID() == 9999) {
                            System.out.println("Unreachable:" + f);
                        } else if (f.getDestination().getIPv4Address() == 0
                                && f.getDestination().getSubSystemID() == NetworkNodeConnection.DESTINATION_BROADCAST) {
                            System.out.println("Broadcast:" + f);
                        } else if (f.getDestination().getIPv4Address() == 0
                                && f.getDestination().getSubSystemID() > NetworkNodeConnection.DESTINATION_BROADCAST) {
                            System.out.println("Multicast:" + f);
                        } else
                            System.out.println("Regular:" + f);
                    }
                }
            }
        }
    }
}
