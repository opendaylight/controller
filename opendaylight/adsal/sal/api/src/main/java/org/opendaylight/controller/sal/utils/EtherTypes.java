
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
 * The enum contains the most common 802.3 ethernet types and 802.2 + SNAP protocol ids
 *
 *
 *
 */
public enum EtherTypes {
    PVSTP("PVSTP", 0x010B), // 802.2 + SNAP (Spanning Tree)
    CDP("CDP", 0x2000), // 802.2 + SNAP
    VTP("VTP", 0x2003), // 802.2 + SNAP
    IPv4("IPv4", 0x800), ARP("ARP", 0x806), RARP("Reverse ARP", 0x8035), VLANTAGGED(
            "VLAN Tagged", 0x8100), // 802.1Q
    IPv6("IPv6", 0x86DD), MPLSUCAST("MPLS Unicast", 0x8847), MPLSMCAST(
            "MPLS Multicast", 0x8848), QINQ("QINQ", 0x88A8), // Standard 802.1ad QinQ
    LLDP("LLDP", 0x88CC), OLDQINQ("Old QINQ", 0x9100), // Old non-standard QinQ
    CISCOQINQ("Cisco QINQ", 0x9200); // Cisco non-standard QinQ

    private static final String regexNumberString = "^[0-9]+$";
    private String description;
    private int number;

    private EtherTypes(String description, int number) {
        this.description = description;
        this.number = number;
    }

    public String toString() {
        return description;
    }

    public int intValue() {
        return number;
    }

    public short shortValue() {
        return ((Integer) number).shortValue();
    }

    public static String getEtherTypeName(int number) {
        return getEtherTypeInternal(number);
    }

    public static String getEtherTypeName(short number) {
        return getEtherTypeInternal((int) number & 0xffff);
    }

    public static String getEtherTypeName(byte number) {
        return getEtherTypeInternal((int) number & 0xff);
    }

    private static String getEtherTypeInternal(int number) {
        for (EtherTypes type : EtherTypes.values()) {
            if (type.number == number) {
                return type.toString();
            }
        }
        return "0x" + Integer.toHexString(number);
    }

    public static short getEtherTypeNumberShort(String name) {
        if (name.matches(regexNumberString)) {
            return Short.valueOf(name);
        }
        for (EtherTypes type : EtherTypes.values()) {
            if (type.description.equalsIgnoreCase(name)) {
                return type.shortValue();
            }
        }
        return 0;
    }

    public static int getEtherTypeNumberInt(String name) {
        if (name.matches(regexNumberString)) {
            return Integer.valueOf(name);
        }
        for (EtherTypes type : EtherTypes.values()) {
            if (type.description.equalsIgnoreCase(name)) {
                return type.intValue();
            }
        }
        return 0;
    }

    public static List<String> getEtherTypesNameList() {
        List<String> ethertypesList = new ArrayList<String>();
        for (EtherTypes type : EtherTypes.values()) {
            ethertypesList.add(type.toString());
        }
        return ethertypesList;
    }

    public static EtherTypes loadFromString(String string) {
        int intType = Integer.parseInt(string);

        for (EtherTypes type : EtherTypes.values()) {
            if (type.number == intType) {
                return type;
            }
        }
        return null;
    }

}
