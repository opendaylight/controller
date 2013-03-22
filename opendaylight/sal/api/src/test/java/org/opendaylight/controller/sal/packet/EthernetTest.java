
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.packet;

import junit.framework.Assert;

import org.junit.Test;
import org.opendaylight.controller.sal.packet.Ethernet;

public class EthernetTest {

    @Test
    public void testGetDestinationMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 10, 12, 14, 20, 55, 69 };
        eth.hdrFieldsMap.put("DestinationMACAddress", mac);
        byte[] dMAC = eth.getDestinationMACAddress();
        Assert.assertTrue(dMAC[0] == 10);
        Assert.assertTrue(dMAC[1] == 12);
        Assert.assertTrue(dMAC[2] == 14);
        Assert.assertTrue(dMAC[3] == 20);
        Assert.assertTrue(dMAC[4] == 55);
        Assert.assertTrue(dMAC[5] == 69);

    }

    @Test
    public void testSourceMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 120, 30, 25, 80, 66, 99 };
        eth.hdrFieldsMap.put("SourceMACAddress", mac);
        byte[] sMAC = eth.getSourceMACAddress();
        Assert.assertTrue(sMAC[0] == 120);
        Assert.assertTrue(sMAC[1] == 30);
        Assert.assertTrue(sMAC[2] == 25);
        Assert.assertTrue(sMAC[3] == 80);
        Assert.assertTrue(sMAC[4] == 66);
        Assert.assertTrue(sMAC[5] == 99);

    }

    @Test
    public void testGetEthertype() throws Exception {
        Ethernet eth = new Ethernet();
        byte ethType[] = { 8, 6 };
        eth.hdrFieldsMap.put("EtherType", ethType);
        short etherType = eth.getEtherType();
        Assert.assertTrue(etherType == 2054);
    }

    @Test
    public void testSetDestinationMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 10, 12, 14, 20, 55, 69 };
        eth.setDestinationMACAddress(mac);
        byte[] dMAC = eth.hdrFieldsMap.get("DestinationMACAddress");
        Assert.assertTrue(dMAC[0] == 10);
        Assert.assertTrue(dMAC[1] == 12);
        Assert.assertTrue(dMAC[2] == 14);
        Assert.assertTrue(dMAC[3] == 20);
        Assert.assertTrue(dMAC[4] == 55);
        Assert.assertTrue(dMAC[5] == 69);

    }

    @Test
    public void testSetSourceMACAddress() {
        Ethernet eth = new Ethernet();
        byte mac[] = { 120, 30, 25, 80, 66, 99 };
        eth.setSourceMACAddress(mac);
        byte[] sMAC = eth.hdrFieldsMap.get("SourceMACAddress");
        Assert.assertTrue(sMAC[0] == 120);
        Assert.assertTrue(sMAC[1] == 30);
        Assert.assertTrue(sMAC[2] == 25);
        Assert.assertTrue(sMAC[3] == 80);
        Assert.assertTrue(sMAC[4] == 66);
        Assert.assertTrue(sMAC[5] == 99);

    }

    @Test
    public void testSetEthertype() throws Exception {
        Ethernet eth = new Ethernet();
        short ethType = 2054;
        eth.setEtherType(ethType);
        byte[] etherType = eth.hdrFieldsMap.get("EtherType");
        Assert.assertTrue(etherType[0] == 8);
        Assert.assertTrue(etherType[1] == 6);

    }

}
