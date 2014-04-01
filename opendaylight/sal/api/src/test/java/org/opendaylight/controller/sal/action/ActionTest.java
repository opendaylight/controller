
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import org.opendaylight.controller.sal.core.ConstructionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.Assert;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionTest {
    protected static final Logger logger = LoggerFactory.getLogger(ActionTest.class);
    private static List<Class<? extends Action>> actionClassesList = new ArrayList<Class<? extends Action>>();
    static {
        actionClassesList.add(Loopback.class);
        actionClassesList.add(Drop.class);
        actionClassesList.add(Flood.class);
        actionClassesList.add(FloodAll.class);
        actionClassesList.add(SwPath.class);
        actionClassesList.add(HwPath.class);
        actionClassesList.add(Controller.class);
        actionClassesList.add(Output.class);
        actionClassesList.add(Enqueue.class);
        actionClassesList.add(PushVlan.class);
        actionClassesList.add(SetVlanId.class);
        actionClassesList.add(SetVlanCfi.class);
        actionClassesList.add(SetVlanPcp.class);
        actionClassesList.add(PopVlan.class);
        actionClassesList.add(SetDlSrc.class);
        actionClassesList.add(SetDlDst.class);
        actionClassesList.add(SetDlType.class);
        actionClassesList.add(SetNwSrc.class);
        actionClassesList.add(SetNwDst.class);
        actionClassesList.add(SetTpSrc.class);
        actionClassesList.add(SetTpDst.class);
    }

    @Test
    public void testActionNamePatrerns() throws IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException {
        Set<String> actionsNames = new HashSet<String>();

        Class<?> emptyParameterArray[] = new Class<?>[0];
        for (Class<? extends Action> actionClass : actionClassesList) {
            Constructor<? extends Action> defaultConstructor = actionClass.getConstructor(emptyParameterArray);
            Action action = defaultConstructor.newInstance();
            actionsNames.add(action.getName());
        }

        Assert.assertTrue(actionsNames.size() == actionClassesList.size());
    }

    @Test
    public void testActionStringParsing() {
        Node node = NodeCreator.createOFNode(2L);
        Assert.assertNotNull(new Controller().fromString("ConTroLlEr", node));
        Assert.assertNotNull(new Loopback().fromString("LoopbAcK", node));
        Assert.assertNotNull(new Drop().fromString("dRoP", node));
        Assert.assertNotNull(new PopVlan().fromString("pop_vlaN", node));
        Assert.assertNotNull(new Flood().fromString("FLoOd", node));
        Assert.assertNotNull(new FloodAll().fromString("FLOOD_All", node));
        Assert.assertNotNull(new HwPath().fromString(" hw_pAtH", node));
        Assert.assertNotNull(new SwPath().fromString("SW_PATH ", node));

        NodeConnector nodeConnector = NodeConnectorCreator.createOFNodeConnector((short)3, node);
        Output output = new Output().fromString("OutpuT= 3", node);
        Assert.assertNotNull(output);
        Assert.assertTrue(output.isValid());
        Assert.assertEquals(output.getPort(), nodeConnector);

        Enqueue enqueue = new Enqueue().fromString("EnqueuE =3", node);
        Assert.assertNotNull(enqueue);
        Assert.assertTrue(enqueue.isValid());
        Assert.assertEquals(enqueue.getPort(), nodeConnector);
        Assert.assertTrue(enqueue.getQueue() == 0);

        enqueue = new Enqueue().fromString("EnqueuE=(3#2)", node);
        Assert.assertNotNull(enqueue);
        Assert.assertTrue(enqueue.isValid());
        Assert.assertEquals(enqueue.getPort(), nodeConnector);
        Assert.assertTrue(enqueue.getQueue() == 2);

        SetDlSrc setDlSrc = new SetDlSrc().fromString("SET_DL_SRC=0b:a9:c8:34:5f:22", node);
        Assert.assertNotNull(setDlSrc);
        Assert.assertTrue(setDlSrc.isValid());
        Assert.assertEquals(setDlSrc.getDlAddressString(), "0b:a9:c8:34:5f:22");

        SetDlDst setDlDst = new SetDlDst().fromString("SET_DL_DST=00:10:a0:bc:ed:b5", node);
        Assert.assertNotNull(setDlDst);
        Assert.assertTrue(setDlDst.isValid());
        Assert.assertEquals(setDlDst.getDlAddressString(), "00:10:a0:bc:ed:b5");

        SetDlType setDlType = new SetDlType().fromString("SET_DL_TYPE=0x800", node);
        Assert.assertNotNull(setDlType);
        Assert.assertTrue(setDlType.isValid());
        Assert.assertTrue(setDlType.getDlType() == 0x800);

        SetVlanId setVlanId = new SetVlanId().fromString("SET_VLAN_ID=2078", node);
        Assert.assertNotNull(setVlanId);
        Assert.assertTrue(setVlanId.isValid());
        Assert.assertTrue(setVlanId.getVlanId() == 2078);

        PushVlan pushVlan = new PushVlan().fromString("PUSH_VLAN=(0x8100:4:1:2078)", node);
        Assert.assertNotNull(pushVlan);
        Assert.assertTrue(pushVlan.isValid());
        Assert.assertTrue(pushVlan.getTag() == 0x8100);
        Assert.assertTrue(pushVlan.getPcp() == 4);
        Assert.assertTrue(pushVlan.getCfi() == 1);
        Assert.assertTrue(pushVlan.getVlanId() == 2078);

        SetNwSrc setNwSrc = new SetNwSrc().fromString("SET_NW_SRC=192.168.3.53", node);
        Assert.assertNotNull(setNwSrc);
        Assert.assertTrue(setNwSrc.isValid());
        Assert.assertEquals(setNwSrc.getAddressAsString(), "192.168.3.53");

        SetNwDst setNwDst = new SetNwDst().fromString("SET_NW_DST=202.168.3.53", node);
        Assert.assertNotNull(setNwDst);
        Assert.assertTrue(setNwDst.isValid());
        Assert.assertEquals(setNwDst.getAddressAsString(), "202.168.3.53");

        Assert.assertNull(new Controller().fromString(" CON TROLLER ", node));
        Assert.assertNull(new Loopback().fromString("LLOOPBACK", node));
        Assert.assertNull(new Drop().fromString("DROPP", node));
        Assert.assertNull(new PopVlan().fromString("POP__VLAN", node));
        Assert.assertNull(new Flood().fromString("FL OOD", node));
        Assert.assertNull(new FloodAll().fromString("FLOOD_Alll", node));
        Assert.assertNull(new HwPath().fromString(" hw_pAtHh", node));
        Assert.assertNull(new SwPath().fromString("SW__PATH ", node));
    }

    @Test
    public void testActionParsingToStringInverse() throws InstantiationException, IllegalAccessException {
        Node node = null;
        List<String> actionStrings = new ArrayList<String>();
        actionStrings.add("LOOPBACK");
        actionStrings.add("FLOOD");
        actionStrings.add("FLOOD_ALL");
        actionStrings.add("SW_PATH");
        actionStrings.add("HW_PATH");
        actionStrings.add("CONTROLLER");
        actionStrings.add("OUTPUT=OF|24@OF|00:00:23:00:89:A2:CC:02");
        actionStrings.add("ENQUEUE=(OF|24@OF|00:00:23:00:89:A2:CC:02#3)");
        actionStrings.add("ENQUEUE=OF|24@OF|00:00:23:00:89:A2:CC:05");
        actionStrings.add("POP_VLAN");
        actionStrings.add("SET_VLAN_ID=57");
        actionStrings.add("PUSH_VLAN=(0x8100:6:1:674)");
        actionStrings.add("SET_DL_SRC=0b:a9:c8:34:5f:22");
        actionStrings.add("SET_DL_dst=0b:a9:c8:34:5f:33");
        actionStrings.add("SET_VLAN_CFI=1");
        actionStrings.add("SET_VLAN_PCP=5");
        actionStrings.add("SET_NW_SRC=192.168.3.53");
        actionStrings.add("SET_NW_DST=202.168.3.53");
        actionStrings.add("SET_TP_SRC=1021");
        actionStrings.add("SET_TP_DST=50210");

        for (String actionString : actionStrings) {
            Action action = null;
            for (Class<? extends Action> actionClass : actionClassesList) {
                action = actionClass.newInstance().fromString(actionString, node);
                if (action != null) {
                    break;
                }
            }
            Assert.assertNotNull(action);
            Assert.assertTrue(action.toString().equalsIgnoreCase(actionString));
        }
    }

    @Test
    public void testActionCreationValidation() {
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
}
