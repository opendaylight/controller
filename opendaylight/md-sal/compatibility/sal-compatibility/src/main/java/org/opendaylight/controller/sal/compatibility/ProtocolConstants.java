/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

public class ProtocolConstants {
    // source: http://en.wikipedia.org/wiki/Ethertype
    public static final short ETHERNET_ARP = (short) 0x0806;

    // source: http://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
    public static final byte TCP = (byte) 0x06;
    public static final byte UDP = (byte) 0x11;
    public static final byte CRUDP = (byte) 0x7F;

    private ProtocolConstants() {

    }
}
