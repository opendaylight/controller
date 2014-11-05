/*
 * Copyright (c) 2013-2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.action;

import org.opendaylight.controller.sal.core.ConstructionException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.Assert;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Tables;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionTest {
    protected static final Logger logger = LoggerFactory
    .getLogger(ActionTest.class);
    @Test
    public void tesActionCreationValidation() {
        Action action = new PopVlan();
        Assert.assertTrue(action.isValid());

        byte mac[] = { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0x11,
                (byte) 0x22, (byte) 0x33 };

        action = new SetDlSrc(mac);
        Assert.assertTrue(action.isValid());

        action = new SetDlSrc(mac);
        Assert.assertTrue(action.isValid());
    }

    @Test
    public void testSetVlanActionCreation() {
        Action action = null;

        action = new SetVlanId(2);
        Assert.assertTrue(action.isValid());

        action = new SetVlanId(4095);
        Assert.assertTrue(action.isValid());

        action = new SetVlanId(0);
        Assert.assertFalse(action.isValid());

        action = new SetVlanId(1);
        Assert.assertTrue(action.isValid());

        action = new SetVlanId(4096);
        Assert.assertFalse(action.isValid());
    }

    @Test
    public void testPushVlanActionCreation() {
        Action action = null;

        action = new PushVlan(EtherTypes.QINQ, 0x4, 0x1, 2000);
        Assert.assertTrue(action.isValid());

        action = new PushVlan(EtherTypes.QINQ.intValue(), 0x4, 0x1, 2000);
        Assert.assertTrue(action.isValid());

        action = new PushVlan(EtherTypes.OLDQINQ, 0x4, 2, 2000);
        Assert.assertFalse(action.isValid());

        action = new PushVlan(EtherTypes.VLANTAGGED, 0x4, 0, 2000);
        Assert.assertTrue(action.isValid());

        action = new PushVlan(EtherTypes.QINQ.intValue(), 0x4, 0x1, 5000);
        Assert.assertFalse(action.isValid());

        action = new PushVlan(EtherTypes.LLDP, 0x4, 0x1, 2000);
        Assert.assertFalse(action.isValid());

        action = new PushVlan(EtherTypes.PVSTP, 0x4, 2, 2000);
        Assert.assertFalse(action.isValid());

        action = new PushVlan(EtherTypes.QINQ, 0x4, -1, 2000);
        Assert.assertFalse(action.isValid());

        // OF 1.3 PUSH_VLAN test.
        for (EtherTypes tag: EtherTypes.values()) {
            int t = tag.intValue();
            boolean valid =
                (tag == EtherTypes.VLANTAGGED || tag == EtherTypes.QINQ);
            PushVlan pv = new PushVlan(tag);
            Assert.assertEquals(valid, pv.isValid());
            if (valid) {
                Assert.assertEquals(t, pv.getTag());
            }

            pv = new PushVlan(t);
            Assert.assertEquals(valid, pv.isValid());
            if (valid) {
                Assert.assertEquals(t, pv.getTag());
            }
        }
    }

    @Test
    public void testSetVlanPcpActionCreation() {
        Action action = null;

        action = new SetVlanPcp(0x4);
        Assert.assertTrue(action.isValid());

        action = new SetVlanPcp(0x8);
        Assert.assertFalse(action.isValid());

        action = new SetVlanPcp(-1);
        Assert.assertFalse(action.isValid());
    }

    @Test
    public void testSetVlanCfiActionCreation() {
        Action action = null;

        action = new SetVlanCfi(0x0);
        Assert.assertTrue(action.isValid());

        action = new SetVlanCfi(0x1);
        Assert.assertTrue(action.isValid());

        action = new SetVlanCfi(0x2);
        Assert.assertFalse(action.isValid());

        action = new SetVlanCfi(-1);
        Assert.assertFalse(action.isValid());
    }

    @Test
    public void testNetworkSetActionCreation() {
        Action action = null;

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("171.71.9.52");
        } catch (UnknownHostException e) {
            logger.error("",e);
        }

        action = new SetNwSrc(ip);
        Assert.assertTrue(action.isValid());

        action = new SetNwDst(ip);
        Assert.assertTrue(action.isValid());

        try {
            ip = InetAddress.getByName("2001:420:281:1003:f2de:f1ff:fe71:728d");
        } catch (UnknownHostException e) {
            logger.error("", e);
        }
        action = new SetNwSrc(ip);
        Assert.assertTrue(action.isValid());

        action = new SetNwDst(ip);
        Assert.assertTrue(action.isValid());

        action = new SetNwTos(0xf);
        Assert.assertTrue(action.isValid());

        action = new SetNwTos(0x3f);
        Assert.assertTrue(action.isValid());

        action = new SetNwTos(0x40);
        Assert.assertFalse(action.isValid());

        action = new SetNwTos(0xff1);
        Assert.assertFalse(action.isValid());

        action = new SetNwTos(-1);
        Assert.assertFalse(action.isValid());
    }

    @Test
    public void testTransportSetActionCreation() {
        Action action = null;

        action = new SetTpSrc(50000);
        Assert.assertTrue(action.isValid());

        action = new SetTpDst(65535);
        Assert.assertTrue(action.isValid());

        action = new SetTpSrc(0);
        Assert.assertTrue(action.isValid());

        action = new SetTpDst(0);
        Assert.assertTrue(action.isValid());

        action = new SetTpSrc(-1);
        Assert.assertFalse(action.isValid());

        action = new SetTpDst(-1);
        Assert.assertFalse(action.isValid());

        action = new SetTpSrc(65536);
        Assert.assertFalse(action.isValid());

        action = new SetTpDst(65536);
        Assert.assertFalse(action.isValid());
    }

    @Test
    public void testNextHopActionCreation() {
        SetNextHop action = null;

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("171.71.9.52");
        } catch (UnknownHostException e) {
            logger.error("", e);
        }

        action = new SetNextHop(ip);
        Assert.assertTrue(action.getAddress().equals(ip));

        try {
            ip = InetAddress.getByName("2001:420:281:1003:f2de:f1ff:fe71:728d");
        } catch (UnknownHostException e) {
            logger.error("", e);
        }
        action = new SetNextHop(ip);
        Assert.assertTrue(action.getAddress().equals(ip));
    }

    @Test
    public void testActionList() {
        List<Action> actions = new ArrayList<Action>();
        short portId = (short) 9;
        Node node = null;
        try {
            node = new Node(Node.NodeIDType.OPENFLOW, new Long(0x55667788L));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
        NodeConnector nc = NodeConnectorCreator.createNodeConnector(portId,
                node);
        byte mac[] = { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0x11,
                (byte) 0x22, (byte) 0x33 };
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("1.1.1.1");
        } catch (UnknownHostException e) {
            logger.error("",e);
        }

        actions.add(new SetDlSrc(mac));
        actions.add(new SetNwSrc(ip));
        actions.add(new Output(nc));
        Assert.assertTrue(actions.size() == 3);
        Assert.assertTrue(actions.get(0).isValid());

        Action probe = new Output(nc);
        Assert.assertTrue(actions.contains(probe));
        Assert.assertFalse(actions.contains(new Output(NodeConnectorCreator
                .createNodeConnector((short) 5, node))));
        Assert.assertFalse(actions.contains(new Controller()));
    }

    @Test
    public void testMetadata() {
        Property tier1 = new Tier(1);
        Property tier2 = new Tier(2);
        Property table1 = new Tables((byte)0x7f);
        Action a1 = new PopVlan();
        List<Property> resprops = null;
        resprops = a1.getMetadatas();
        // This should be an empty list
        Assert.assertTrue(resprops.isEmpty());
        a1.setMetadata("tier1", tier1);
        a1.setMetadata("tier2", tier2);
        a1.setMetadata("table1", table1);
        resprops = a1.getMetadatas();
        // Check for the number of elements in it
        Assert.assertTrue(resprops.size() == 3);
        // Check if the elements are in it
        Assert.assertTrue(resprops.contains(tier1));
        Assert.assertTrue(resprops.contains(tier2));
        Assert.assertTrue(resprops.contains(table1));
        // Check for single elements retrieve
        Assert.assertTrue(a1.getMetadata("tier1").equals(tier1));
        Assert.assertTrue(a1.getMetadata("tier2").equals(tier2));
        Assert.assertTrue(a1.getMetadata("table1").equals(table1));
        // Now remove an element and make sure the remaining are
        // correct
        a1.removeMetadata("tier1");

        resprops = a1.getMetadatas();
        // Check for the number of elements in it
        Assert.assertTrue(resprops.size() == 2);
        // Check if the elements are in it
        Assert.assertFalse(resprops.contains(tier1));
        Assert.assertTrue(resprops.contains(tier2));
        Assert.assertTrue(resprops.contains(table1));
        // Check for single elements retrieve
        Assert.assertTrue(a1.getMetadata("table1").equals(table1));
        Assert.assertTrue(a1.getMetadata("tier2").equals(tier2));
        Assert.assertNull(a1.getMetadata("tier1"));

        // Check for an element never existed
        Assert.assertNull(a1.getMetadata("table100"));

        // Remove them all
        a1.removeMetadata("tier2");
        a1.removeMetadata("table1");

        // Remove also a non-existent one
        a1.removeMetadata("table100");

        resprops = a1.getMetadatas();
        // Check there are no elements left
        Assert.assertTrue(resprops.size() == 0);

        // Now check for exception on setting null values
        try {
            a1.setMetadata("foo", null);
            // The line below should never be reached
            Assert.assertTrue(false);
        } catch (NullPointerException nue) {
            // NPE should be raised for null value
            Assert.assertTrue(true);
        }

        // Now check on using null key
        try {
            a1.setMetadata(null, table1);
            // The line below should never be reached
            Assert.assertTrue(false);
        } catch (NullPointerException nue) {
            // NPE should be raised for null value
            Assert.assertTrue(true);
        }

        // Now check on using null key and null value
        try {
            a1.setMetadata(null, null);
            // The line below should never be reached
            Assert.assertTrue(false);
        } catch (NullPointerException nue) {
            // NPE should be raised for null value
            Assert.assertTrue(true);
        }
    }
}
