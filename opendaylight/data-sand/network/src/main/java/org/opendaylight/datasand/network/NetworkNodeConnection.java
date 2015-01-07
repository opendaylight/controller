package org.opendaylight.datasand.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import org.opendaylight.datasand.codec.bytearray.ByteEncoder;

public class NetworkNodeConnection extends Thread {

    public static final int DESTINATION_UNREACHABLE = 9999;
    public static final int DESTINATION_BROADCAST = 10;

    public static final NetworkID PROTOCOL_ID_UNREACHABLE = new NetworkID(0, 0,DESTINATION_UNREACHABLE);
    public static final NetworkID PROTOCOL_ID_BROADCAST = new NetworkID(0, 0,DESTINATION_BROADCAST);

    private Socket socket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private PriorityLinkedList<byte[]> incoming = new PriorityLinkedList<byte[]>();
    private NetworkNode networkNode = null;
    private boolean running = false;
    private boolean valideConnection = false;
    private String connectionString = null;

    public NetworkNodeConnection(NetworkNode _nn, InetAddress addr, int port) {
        try {
            this.networkNode = _nn;
            this.setName("Con of-" + _nn.getName());
            socket = new Socket(addr, port);
            try {
                in = new DataInputStream(new BufferedInputStream(
                        this.socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(
                        socket.getOutputStream()));
                String conString = "C:" + networkNode.getLocalHost().getPort()
                        + "    ";
                out.write(conString.getBytes());
                out.flush();
                byte data[] = new byte[10];
                in.readFully(data);
                connectionString = new String(data).trim();
                this.valideConnection = true;
                this.start();
            } catch (Exception err) {
                err.printStackTrace();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public NetworkNodeConnection(NetworkNode _nn, Socket _s) {
        this.socket = _s;
        this.networkNode = _nn;
        this.setName("Con of-" + _nn.getName());
        try {
            in = new DataInputStream(new BufferedInputStream(
                    this.socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(
                    this.socket.getOutputStream()));
            byte data[] = new byte[10];
            in.readFully(data);
            connectionString = new String(data);
            String conString = "C:" + networkNode.getLocalHost().getPort()
                    + "    ";
            out.write(conString.getBytes());
            out.flush();
            this.valideConnection = true;
            this.start();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public byte[] sendPacket(byte[] data) throws IOException {
        synchronized (out) {
            if(running){
                try{
                    out.writeInt(data.length);
                    out.write(data);
                    out.flush();
                    return null;
                }catch(SocketException serr){
                    System.out.println("Connection was probably terminated for "+socket.getInetAddress().getHostName()+":"+socket.getPort());
                    this.shutdown();
                    return markAsUnreachable(data);
                }
            }else{
                return markAsUnreachable(data);
            }
        }
    }

    public void shutdown() {
        this.running = false;
        try {
            this.socket.close();
        } catch (Exception err) {
        }
    }

    public void run() {
        NetworkID id = null;
        synchronized (networkNode.incomingConnections) {
            if (!valideConnection)
                return;
            if (connectionString.startsWith("R")) {
                InetAddress source = this.socket.getInetAddress();
                int port = Integer.parseInt(connectionString.substring(
                        connectionString.indexOf(":") + 1).trim());
                String sourceAddress = source.getHostAddress();
                if (sourceAddress.equals("127.0.0.1")) {
                    id = new NetworkID(networkNode.getLocalHost()
                            .getIPv4Address(), port, 0);
                } else
                    id = NetworkID.valueOf(source.getHostAddress() + ":" + port
                            + ":0");
                if (!networkNode.incomingConnections.contains(id)) {
                    new NetworkNodeConnection(this.networkNode, source, port);
                    networkNode.incomingConnections.add(id);
                }
                return;
            } else {
                InetAddress source = this.socket.getInetAddress();
                int port = Integer.parseInt(connectionString.substring(
                        connectionString.indexOf(":") + 1).trim());
                String sourceAddress = source.getHostAddress();
                if (sourceAddress.equals("127.0.0.1")) {
                    id = new NetworkID(networkNode.getLocalHost()
                            .getIPv4Address(), port, 0);
                } else
                    id = NetworkID.valueOf(source.getHostAddress() + ":" + port
                            + ":0");

            }
        }

        if (!networkNode.registerNetworkNodeConnection(this, id)) {
            return;
        }

        running = true;

        new Switch();

        try {
            if (in.available() > 0) {
                byte data[] = new byte[in.available()];
                in.readFully(data);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        try {
            while (socket != null && !socket.isClosed() && running) {
                int size = in.readInt();
                byte data[] = new byte[size];
                in.readFully(data);
                synchronized (incoming) {
                    incoming.add(
                            data,
                            data[Packet.PACKET_MULTIPART_AND_PRIORITY_LOCATION] / 2);
                    incoming.notifyAll();
                }
            }
        } catch (Exception err) {
            // err.printStackTrace();
        }
        System.out.println(this.getName()+" was closed.");
    }

    public static final byte[] markAsUnreachable(byte[] data) {
        byte[] mark = new byte[data.length + Packet.PACKET_DATA_LOCATION];
        System.arraycopy(data, 0, mark, 0, Packet.PACKET_DATA_LOCATION);
        System.arraycopy(data, 0, mark, Packet.PACKET_DATA_LOCATION,data.length);
        System.arraycopy(mark, Packet.PACKET_SOURCE_LOCATION, mark,Packet.PACKET_DEST_LOCATION, 8);
        PROTOCOL_ID_UNREACHABLE.encode(PROTOCOL_ID_UNREACHABLE, mark,Packet.PACKET_SOURCE_LOCATION);
        return mark;
    }

    public static final byte[] addUnreachableAddressForMulticast(byte data[],int destAddress,int destPort){
        byte[] _unreachable = new byte[data.length+6];
        System.arraycopy(data, 0, _unreachable,0, data.length);
        ByteEncoder.encodeInt32(destAddress, _unreachable, data.length);
        ByteEncoder.encodeInt16(destPort, _unreachable, data.length+4);
        return _unreachable;
    }

    private class Switch extends Thread {

        public Switch() {
            this.setName("Switch - " + NetworkNodeConnection.this.getName());
            this.start();
        }

        public void run() {
            while (running) {
                byte data[] = null;
                synchronized (incoming) {
                    if (incoming.size() == 0) {
                        try {
                            incoming.wait(5000);
                        } catch (Exception err) {
                        }
                    }
                    if (incoming.size() > 0) {
                        data = incoming.next();
                    }
                }

                if (data != null && data.length > 0) {
                    int destAddr = ByteEncoder.decodeInt32(data,Packet.PACKET_DEST_LOCATION);
                    int destPort = ByteEncoder.decodeInt16(data,Packet.PACKET_DEST_LOCATION + 4);
                    if (destAddr == 0) {
                        if (networkNode.getLocalHost().getPort() != 50000) {
                            networkNode.receivedPacket(data);
                        } else {
                            networkNode.broadcast(data);
                        }
                    } else if (destAddr == networkNode.getLocalHost().getIPv4Address() && destPort == networkNode.getLocalHost().getPort()) {
                        networkNode.receivedPacket(data);
                    } else if (destAddr == networkNode.getLocalHost().getIPv4Address() && networkNode.getLocalHost().getPort() == 50000 && destPort != 50000) {
                        NetworkNodeConnection other = networkNode.getNodeConnection(destAddr, destPort);
                        if (other != null && other.running) {
                            try {
                                other.sendPacket(data);
                            } catch (Exception err) {
                                err.printStackTrace();
                            }
                        } else {
                            data = markAsUnreachable(data);
                            destAddr = ByteEncoder.decodeInt32(data,Packet.PACKET_DEST_LOCATION);
                            destPort = ByteEncoder.decodeInt16(data,Packet.PACKET_DEST_LOCATION + 4);
                            NetworkNodeConnection source = networkNode.getNodeConnection(destAddr, destPort);
                            if (source != null) {
                                try {
                                    source.sendPacket(data);
                                } catch (Exception err) {
                                    err.printStackTrace();
                                }
                            } else
                                System.err.println("Source unreachable:"
                                        + new NetworkID(destAddr, destPort,
                                                ByteEncoder.decodeInt16(data,
                                                        16)));
                        }
                    } else if (destAddr != networkNode.getLocalHost().getIPv4Address() && networkNode.getLocalHost().getPort() == 50000) {
                        NetworkNodeConnection other = networkNode.getNodeConnection(destAddr, 50000);
                        if (other != null) {
                            try {
                                other.sendPacket(data);
                            } catch (Exception err) {
                                err.printStackTrace();
                            }
                        } else {
                            data = markAsUnreachable(data);
                            destAddr = ByteEncoder.decodeInt32(data,Packet.PACKET_DEST_LOCATION);
                            destPort = ByteEncoder.decodeInt16(data,Packet.PACKET_DEST_LOCATION + 4);
                            NetworkNodeConnection source = networkNode.getNodeConnection(destAddr, destPort);
                            if (source != null) {
                                try {
                                    source.sendPacket(data);
                                } catch (Exception err) {
                                    err.printStackTrace();
                                }
                            } else
                                System.err.println("Source unreachable:"
                                        + new NetworkID(destAddr, destPort,
                                                ByteEncoder.decodeInt16(data,
                                                        16)));
                        }
                    }

                }
            }
            System.out.println(this.getName()+" end.");
        }
    }
}