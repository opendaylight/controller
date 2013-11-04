package org.opendaylight.controller.sal.compatibility;

public class ProtocolConstants {
    // source: http://en.wikipedia.org/wiki/Ethertype
    public static final short ETHERNET_ARP = (short) 0x0806;

    // source: http://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
    public static final byte TCP = (byte) 0x06;
    public static final byte UDP = (byte) 0x11;
    public static final byte SCTP = (byte) 0x84;

    private ProtocolConstants() {

    }
}
