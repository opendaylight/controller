
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Enum represents the most common IP protocols numbers It provides the binding
 * between IP protocol names and numbers and provides APIs to read and parse
 * them in either of the two forms
 *
 * NOTE: Openflow 1.0 supports the IP Proto match only for ICMP, TCP and UDP
 *
 * references:
 * http://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
 */
@Deprecated
public enum IPProtocols {
     ANY("any", -1),
     HOPOPT("HOPOPT",0),
     ICMP("ICMP", 1),
     IGMP("IGMP",2),
     GGP("GGP",3),
     IPV4("IPv4",4),
     ST("ST",5),
     TCP("TCP", 6),
     CBT("CBT",7),
     EGP("EGP",8),
     IGP("IGP",9),
     BBNRCCMON("BBN-RCC-MON",10),
     NVPII("NVP-II",11),
     PUP("PUP",12),
     ARGUS("ARGUS",13),
     EMCON("EMCON",14),
     XNET("XNET",15),
     CHAOS("CHAOS",16),
     UDP("UDP", 17),
     MUX("MUX",18),
     DCNMEAS("DCN-MEAS",19),
     HMP("HMP",20),
     PRM("PRM",21),
     XNSIDP("XNS-IDP",22),
     TRUNK1("TRUNK-1",23),
     TRUNK2("TRUNK-2",24),
     LEAF1("LEAF-1",25),
     LEAF2("LEAF-2",26),
     RDP("RDP",27),
     IRTP("IRTP",28),
     ISOTP4("ISO-TP4",29),
     NETBLT("NETBLT",30),
     MFENSP("MFE-NSP",31),
     MERITINP("MERIT-INP",32),
     DCCP("DCCP",33),
     THREEPC("3PC",34),
     IDPR("IDPR",35),
     XTP("XTP",36),
     DDP("DDP",37),
     IDPRCMTP("IDPR-CMTP",38),
     TPPLUSPLUS("TP++",39),
     IL("IL",40),
     IPV6("IPv6",41),
     SDRP("SDRP",42),
     IPV6Route("IPv6-Route",43),
     IPV6Frag("IPv6-Frag",44),
     IDRP("IDRP",45),
     RSVP("RSVP",46),
     GRE("GRE",47),
     DSR("DSR",48),
     BNA("BNA",49),
     ESP("ESP",50),
     AH("AH",51),
     INLSP("I-NLSP",52),
     SWIPE("SWIPE",53),
     NARP("NARP",54),
     MOBILE("MOBILE",55),
     TLSP("TLSP",56),
     SKIP("SKIP",57),
     IPV6ICMP("IPv6-ICMP", 58),
     IPV6NoNxt("IPv6-NoNxt",59),
     IPV6Opts("IPv6-Opts",60),
     ANYHOST("ANY-HOST",61),
     CFTP("CFTP",62),
     ANYNETWORK("ANY-NETWORK",63),
     SATEXPAK("SAT-EXPAK",64),
     KRYPTOLAN("KRYPTOLAN",65),
     RVD("RVD",66),
     IPPC("IPPC",67),
     ANYDISTFS("ANY-DIST-FS",68),
     SATMON("SAT-MON",69),
     VISA("VISA",70),
     IPCV("IPCV",71),
     CPNX("CPNX",72),
     CPHB("CPHB",73),
     WSN("WSN",74),
     PVP("PVP",75),
     BRSATMON("BR-SAT-MON",76),
     SUNND("SUN-ND",77),
     WBMON("WB-MON",78),
     WBEXPAK("WB-EXPAK",79),
     ISOIP("ISO-IP",80),
     VMTP("VMTP",81),
     SECUREVMTP("SECURE-VMTP",82),
     VINES("VINES",83),
     TTP("TTP",84),
     IPTM("IPTM",84),
     NSFNETIGP("NSFNET-IGP",85),
     DGP("DGP",86),
     TCF("TCF",87),
     EIGRP("EIGRP",88),
     OSPFIGP("OSPFIGP",89),
     SPRITERPC("Sprite-RPC",90),
     LARP("LARP",91),
     MTP("MTP",92),
     AX25("AX.25",93),
     IPIP("IPIP",94),
     MICP("MICP",95),
     SCCSP("SCC-SP",96),
     ETHERIP("ETHERIP",97),
     ENCAP("ENCAP",98),
     ANYENC("ANY-ENC",99),
     GMTP("GMTP",100),
     IFMP("IFMP",101),
     PNNI("PNNI",102),
     PIM("PIM",103),
     ARIS("ARIS",104),
     SCPS("SCPS",105),
     QNX("QNX",106),
     AN("A/N",107),
     IPComp("IPComp",108),
     SNP("SNP",109),
     COMPAQPEER("Compaq-Peer",110),
     IPXINIP("IPX-in-IP",111),
     VRRP("VRRP",112),
     PGM("PGM",113),
     ANY0HOP("ANY-0-HOP",114),
     L2TP("L2TP",115),
     DDX("DDX",116),
     IATP("IATP",117),
     STP("STP",118),
     SRP("SRP",119),
     UTI("UTI",120),
     SMP("SMP",121),
     SM("SM",122),
     PTP("PTP",123),
     ISIS("ISIS",124),
     FIRE("FIRE",125),
     CRTP("CRTP",126),
     CRUDP("CRUDP",127),
     SSCOPMCE("SSCOPMCE",128),
     IPLT("IPLT",129),
     SPS("SPS",130),
     PIPE("PIPE",131),
     SCTP("SCTP",132),
     FC("FC",133),
     RSVPE2EIGNORE("RSVP-E2E-IGNORE",134),
     MOBILITYHEADER("Mobility Header",135),
     UDPLITE("UDPLite",136),
     MPLSINIP("MPLS-in-IP",137),
     MANET("MANET",138),
     HIP("HIP",139),
     SHIM6("Shim6",140),
     WESP("WESP",141),
     ROHC("ROHC",142),
     /*143-252 Unassigned by IANA*/

     //Experimebtal protocol numbers (http://tools.ietf.org/html/rfc3692)
     EXP1("Experimental1", 253),
     EXP2("Experimental2", 254),

     RESERVED("RESERVED",255);

    private String protocolName;
    private int protocolNumber;

    private IPProtocols(String name, int number) {
        protocolName = name;
        protocolNumber = number;
    }

    public int intValue() {
        return protocolNumber;
    }

    public short shortValue() {
        return ((Integer) protocolNumber).shortValue();
    }

    public byte byteValue() {
        return ((Integer) protocolNumber).byteValue();
    }

    @Override
    public String toString() {
        return protocolName;
    }

    public static String getProtocolName(int number) {
        return getProtocolNameInternal(number);
    }

    public static String getProtocolName(short number) {
        return getProtocolNameInternal(number & 0xffff);
    }

    public static String getProtocolName(byte number) {
        return getProtocolNameInternal(number & 0xff);
    }

    private static String getProtocolNameInternal(int number) {
        for (IPProtocols proto : IPProtocols.values()) {
            if (proto.protocolNumber == number) {
                return proto.toString();
            }
        }
        //TODO: this is for backwards compatibility
        return "0x" + Integer.toHexString(number);
    }

    public static short getProtocolNumberShort(String name) {
        IPProtocols p = fromString(name);
        if (p != null) {
            return p.shortValue();
        }
        //This method should be called after validation only
        throw new IllegalArgumentException("Illegal IP protocol value: " + name);
    }

    public static int getProtocolNumberInt(String name) {
        IPProtocols p = fromString(name);
        if (p != null) {
            return p.intValue();
        }
        //This method should be called after validation only
        throw new IllegalArgumentException("Illegal IP protocol value: " + name);
    }

    public static byte getProtocolNumberByte(String name) {
        IPProtocols p = fromString(name);
        if (p != null) {
            return p.byteValue();
        }
        //This method should be called after validation only
        throw new IllegalArgumentException("Illegal IP protocol value: " + name);
    }

    public static List<String> getProtocolNameList() {
        List<String> protoList = new ArrayList<String>();
        for (IPProtocols proto : IPProtocols.values()) {
            protoList.add(proto.toString());
        }
        return protoList;
    }

    /**
     * Method to parse an IPProtocol from a numeric string
     * (see: {@link Java.Lang.Integer.decode(java.lang.String)} for parsable strings),
     * or this enum's name string.
     *
     * @param s
     *            The IP protocol string to be parsed
     * @return The IP protocol Enum, or null if invalid protocol string is passed
     */
    public static IPProtocols fromString(String s) {
        // null/empty/any/* evaluates to ANY
        if (s == null || s.isEmpty() || s.equalsIgnoreCase("any") || s.equals("*")) {
            return IPProtocols.ANY;
        }

        // Try parsing numeric and find the related ENUM
        try {
            int protoNum = Integer.decode(s);
            for (IPProtocols protoEnum : IPProtocols.values()) {
                if (protoEnum.protocolNumber == protoNum) {
                    return protoEnum;
                }
            }
            // At this point it's an invalid number (i.e. out of range or not a valid proto num)
            return null;
        } catch (NumberFormatException nfe) {
            // numeric failed try by NAME
            try {
                return valueOf(s);
            } catch(IllegalArgumentException e) {
                // Neither numeric nor enum NAME, attempt human readable name
                for (IPProtocols protoEnum : IPProtocols.values()) {
                    if (protoEnum.toString().equalsIgnoreCase(s)) {
                        return protoEnum;
                    }
                }
                //couldn't parse, signifies an invalid proto field!
                return null;
            }

        }
    }
}
