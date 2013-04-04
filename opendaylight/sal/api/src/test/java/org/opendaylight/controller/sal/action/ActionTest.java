
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.PushVlan;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanCfi;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;

public class ActionTest {
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
            e.printStackTrace();
        }

        action = new SetNwSrc(ip);
        Assert.assertTrue(action.isValid());

        action = new SetNwDst(ip);
        Assert.assertTrue(action.isValid());

        try {
            ip = InetAddress.getByName("2001:420:281:1003:f2de:f1ff:fe71:728d");
        } catch (UnknownHostException e) {
            e.printStackTrace();
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

        action = new SetTpDst(0);
        Assert.assertFalse(action.isValid());

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
            e.printStackTrace();
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
}
