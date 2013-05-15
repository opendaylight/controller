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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionTest {
    protected static final Logger logger = LoggerFactory
            .getLogger(ActionTest.class);

    @Test
    public void tesActionCreationValidation() {
        Action action = new PopVlan();
        byte mac[] = { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0x11,
                (byte) 0x22, (byte) 0x33 };
        action = new SetDlSrc(mac);
        action = new SetDlSrc(mac);
    }

    @Test
    public void testSetVlanActionCreation() {
        Action action = null;

        action = new SetVlanId(2);
        action = new SetVlanId(4095);
        action = new SetVlanId(0);
        action = new SetVlanId(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetVlanIdIncorrect1() {
        new SetVlanId(4096);
    }

    @Test
    public void testPushVlanActionCreation() {
        Action action = null;

        action = new PushVlan(EtherTypes.QINQ, 0x4, 0x1, 2000);

        action = new PushVlan(EtherTypes.QINQ.intValue(), 0x4, 0x1, 2000);

        action = new PushVlan(EtherTypes.VLANTAGGED, 0x4, 0, 2000);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushVlanIncorrect1() {
        new PushVlan(EtherTypes.OLDQINQ, 0x4, 2, 2000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushVlanIncorrect2() {
        new PushVlan(EtherTypes.QINQ.intValue(), 0x4, 0x1, 5000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushVlanIncorrect3() {
        new PushVlan(EtherTypes.LLDP, 0x4, 0x1, 2000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushVlanIncorrect4() {
        new PushVlan(EtherTypes.PVSTP, 0x4, 2, 2000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushVlanIncorrect5() {
        new PushVlan(EtherTypes.QINQ, 0x4, -1, 2000);
    }

    @Test
    public void testSetVlanPcpActionCreation() {
        Action action = null;

        action = new SetVlanPcp(0x4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetVlanPcepIncorrect() {
        new SetVlanPcp(0x8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetVlanPcepIncorrect2() {
        new SetVlanPcp(-1);
    }

    @Test
    public void testSetVlanCfiActionCreation() {
        Action action = null;
        action = new SetVlanCfi(0x0);
        action = new SetVlanCfi(0x1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetVlanCfiIncorrect() {
        new SetVlanCfi(0x2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetVlanCfiIncorrect2() {
        new SetVlanCfi(-1);
    }

    @Test
    public void testNetworkSetActionCreation() {
        Action action = null;

        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("171.71.9.52");
        } catch (UnknownHostException e) {
            logger.error("", e);
        }

        action = new SetNwSrc(ip);
        action = new SetNwDst(ip);

        try {
            ip = InetAddress.getByName("2001:420:281:1003:f2de:f1ff:fe71:728d");
        } catch (UnknownHostException e) {
            logger.error("", e);
        }
        action = new SetNwSrc(ip);
        action = new SetNwDst(ip);
        action = new SetNwTos(0xf);
        action = new SetNwTos(0x3f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNwTosIncorrect1() {
        new SetNwTos(0x40);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNwTosIncorrect2() {
        new SetNwTos(0xff1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNwTosIncorrect3() {
        new SetNwTos(-1);
    }

    @Test
    public void testTransportSetActionCreation() {
        Action action = null;

        action = new SetTpSrc(50000);
        action = new SetTpDst(65535);
        action = new SetTpDst(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTpSrcIncorrect1() {
        new SetTpSrc(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTpSrcIncorrect2() {
        new SetTpSrc(65536);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTpDstIncorrect1() {
        new SetTpDst(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTpDstIncorrect2() {
        new SetTpDst(65536);
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
            logger.error("", e);
        }

        actions.add(new SetDlSrc(mac));
        actions.add(new SetNwSrc(ip));
        actions.add(new Output(nc));
        Assert.assertTrue(actions.size() == 3);
        Action probe = new Output(nc);
        Assert.assertTrue(actions.contains(probe));
        Assert.assertFalse(actions.contains(new Output(NodeConnectorCreator
                .createNodeConnector((short) 5, node))));
        Assert.assertFalse(actions.contains(new Controller()));
    }
}
