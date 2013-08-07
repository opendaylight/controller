/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;

public class frmTest {

    @Test
    public void testFlowEntryInstall() throws UnknownHostException {
        Node node = NodeCreator.createOFNode(1L);
        FlowEntry pol = new FlowEntry("polTest", null, getSampleFlowV6(node), node);
        FlowEntry pol2 = new FlowEntry("polTest2", null, getSampleFlowV6(node), node);
        FlowEntryInstall fei = new FlowEntryInstall(pol.clone(), null);
        FlowEntryInstall fei2 = new FlowEntryInstall(pol.clone(), null);
        FlowEntryInstall fei3 = new FlowEntryInstall(pol2.clone(), null);
        Assert.assertTrue(fei.getOriginal().equals(pol));
        Assert.assertTrue(fei.getInstall().equals(pol));
        Assert.assertTrue(fei.getFlowName().equals(pol.getFlowName()));
        Assert.assertTrue(fei.getGroupName().equals(pol.getGroupName()));
        Assert.assertTrue(fei.getNode().equals(pol.getNode()));
        Assert.assertFalse(fei.isDeletePending());
        fei.toBeDeleted();
        Assert.assertTrue(fei.isDeletePending());
        Assert.assertNull(fei.getContainerFlow());
        Assert.assertTrue(fei.equalsByNodeAndName(pol.getNode(), pol.getFlowName()));

        Assert.assertTrue(fei.equals(fei2));
        Assert.assertFalse(fei.equals(null));
        Assert.assertTrue(fei.equals(fei3));

    }

    @Test
    public void testFlowEntryCreation() throws UnknownHostException {
        Node node = NodeCreator.createOFNode(1L);
        FlowEntry pol = new FlowEntry("polTest", null, getSampleFlowV6(node), node);
        Assert.assertTrue(pol.getFlow().equals(getSampleFlowV6(node)));
    }

    @Test
    public void testFlowEntrySetGet() throws UnknownHostException {
        Node node = NodeCreator.createOFNode(1L);
        Node node2 = NodeCreator.createOFNode(2L);
        FlowEntry pol = new FlowEntry("polTest", null, getSampleFlowV6(node), node);
        pol.setGroupName("polTest2");
        pol.setFlowName("flowName");
        Assert.assertTrue(pol.getFlowName().equals("flowName"));
        Assert.assertTrue(pol.getGroupName().equals("polTest2"));
        pol.setNode(node2);
        Assert.assertTrue(pol.getNode().equals(node2));
        Assert.assertTrue(pol.equalsByNodeAndName(node2, "flowName"));
    }

    @Test
    public void testFlowEntryEquality() throws UnknownHostException {
        Node node = NodeCreator.createOFNode(1L);
        Node node2 = NodeCreator.createOFNode(1L);
        FlowEntry pol = new FlowEntry("polTest", null, getSampleFlowV6(node), node);
        FlowEntry pol2 = new FlowEntry("polTest", null, getSampleFlowV6(node), node2);
        Assert.assertTrue(pol.equals(pol2));
    }

    @Test
    public void testFlowEntryCollision() throws UnknownHostException {
        // Create 2 equal FlowEntry objects
        Node node1 = NodeCreator.createOFNode(1L);
        Node node2 = NodeCreator.createOFNode(1L);
        FlowEntry fe1 = new FlowEntry("Junit", "flow1", getSampleFlowV6(node1), node1);
        FlowEntry fe2 = new FlowEntry("Junit", "flow2", getSampleFlowV6(node2), node1);

        // Check equality in FlowEntry and parameters
        Assert.assertTrue(fe1.getFlow().getMatch().equals(fe2.getFlow().getMatch()));
        Assert.assertTrue(fe1.getFlow().getMatch().getMatches() == fe2.getFlow().getMatch().getMatches());
        Assert.assertTrue(fe1.getFlow().getMatch().hashCode() == fe2.getFlow().getMatch().hashCode());
        Assert.assertTrue(fe1.getFlow().hashCode() == fe2.getFlow().hashCode());
        Assert.assertTrue(fe1.equals(fe2));
        Assert.assertTrue(fe1.hashCode() == fe2.hashCode());

        // Change priority field for fe2, verify inequality
        fe2.getFlow().setPriority((short)1000);

        // Verify FlowEntry works as key in collection
        ConcurrentMap<FlowEntry, FlowEntry> map = new ConcurrentHashMap<FlowEntry, FlowEntry>();
        Assert.assertTrue(null == map.put(fe1, fe1));
        Assert.assertTrue(fe1.clone().equals(map.put(fe1.clone(), fe1.clone())));
        Assert.assertTrue(map.get(fe1.clone()).equals(fe1.clone()));
        Assert.assertTrue(map.keySet().contains(fe1.clone()));
        Assert.assertTrue(map.containsKey(fe1));

        // Remove key
        map.remove(fe1);
        Assert.assertTrue(map.isEmpty());
        Assert.assertFalse(map.containsKey(fe1));

        // Verify cloned object as key
        map.put(fe1.clone(), fe1.clone());
        Assert.assertTrue(map.containsKey(fe1));

        // Verify different key is not present
        Assert.assertFalse(map.containsKey(fe2));

        // Add different key
        map.put(fe2.clone(), fe2.clone());
        Assert.assertTrue(map.size() == 2);
        Assert.assertTrue(map.containsKey(fe1));
        Assert.assertTrue(map.containsKey(fe2));

        // Make fe2 equal to fe1 again
        fe2.getFlow().setPriority((short)300);
        Assert.assertTrue(fe2.equals(fe1));
        Assert.assertTrue(map.containsKey(fe2));

        // Clean up
        map.clear();
    }

    @Test
    public void testFlowEntryInstallCollision() throws UnknownHostException {
        // Create 2 equal FlowEntryInstall objects
        Node node1 = NodeCreator.createOFNode(1L);
        Node node2 = NodeCreator.createOFNode(1L);
        FlowEntry fe1 = new FlowEntry("Junit", "flow1", getSampleFlowV6(node1), node1);
        FlowEntry fe2 = new FlowEntry("Junit", "flow2", getSampleFlowV6(node2), node1);
        ContainerFlow cf1 = null;
        ContainerFlow cf2 = null;
        FlowEntryInstall fei1 = new FlowEntryInstall(fe1, cf1);
        FlowEntryInstall fei2 = new FlowEntryInstall(fe2, cf2);

        // Check equality in FlowEntry and parameters
        Assert.assertTrue(fei1.equals(fei2));
        Assert.assertTrue(fei1.hashCode() == fei2.hashCode());

        // Verify FlowEntryInstall works as key in collection
        ConcurrentMap<FlowEntryInstall, FlowEntryInstall> map =
                new ConcurrentHashMap<FlowEntryInstall, FlowEntryInstall>();
        Assert.assertTrue(null == map.put(fei1, fei1));
        Assert.assertTrue(map.get(fei1).equals(fei2));
        Assert.assertTrue(map.keySet().contains(fei1));
        Assert.assertTrue(map.keySet().contains(fei2));
        Assert.assertTrue(map.containsKey(fei1));

        // Remove key
        map.remove(fei1);
        Assert.assertTrue(map.isEmpty());
        Assert.assertFalse(map.containsKey(fei1));

        // Verify cloned object as key
        map.put(fei1, fei1);
        Assert.assertTrue(map.containsKey(fei1));

        // Change fei2, change relevant hashcode info
        fei2.getInstall().getFlow().setPriority((short)301);
        Assert.assertFalse(fei1.equals(fei2));
        Assert.assertFalse(fei1.hashCode() == fei2.hashCode());


        // Verify different key is not present
        Assert.assertFalse(map.containsKey(fei2));

        // Add different key
        map.put(fei2, fei2);
        Assert.assertTrue(map.size() == 2);
        Assert.assertTrue(map.containsKey(fei1));
        Assert.assertTrue(map.containsKey(fei2));

        // Make fei2 equal to fei1 again
        fei2.getInstall().getFlow().setPriority((short)300);
        Assert.assertTrue(fei2.equals(fei1));
        Assert.assertTrue(map.containsKey(fei2));

        // Clean up
        map.clear();
    }

    @Test
    public void testFlowEntryCloning() throws UnknownHostException {
        Node node = NodeCreator.createOFNode(1L);
        FlowEntry pol = new FlowEntry("polTest", null, getSampleFlowV6(node), node);
        FlowEntry pol2 = pol.clone();
        Assert.assertTrue(pol.equals(pol2));
    }

    @Test
    public void testFlowEntrySet() throws UnknownHostException {
        Set<FlowEntry> set = new HashSet<FlowEntry>();

        Node node1 = NodeCreator.createOFNode(1L);
        Node node2 = NodeCreator.createOFNode(2L);
        Node node3 = NodeCreator.createOFNode(3L);

        Match match = new Match();
        match.setField(MatchType.NW_SRC, InetAddress.getAllByName("1.1.1.1"));
        match.setField(MatchType.NW_DST, InetAddress.getAllByName("2.2.2.2"));
        match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());

        List<Action> actionList = new ArrayList<Action>();
        // actionList.add(new Drop());

        Flow flow = new Flow(match, actionList);
        FlowEntry pol1 = new FlowEntry("m1", "same", flow, node1);
        FlowEntry pol2 = new FlowEntry("m2", "same", flow, node2);
        FlowEntry pol3 = new FlowEntry("m3", "same", flow, node3);

        set.add(pol1);
        set.add(pol2);
        set.add(pol3);

        Assert.assertTrue(set.contains(pol1));
        Assert.assertTrue(set.contains(pol2));
        Assert.assertTrue(set.contains(pol3));

        Assert.assertTrue(set.contains(pol1.clone()));
        Assert.assertTrue(set.contains(pol2.clone()));
        Assert.assertTrue(set.contains(pol3.clone()));

    }

    @Test
    public void testInternalFlow() {
        FlowConfig flowConfig = new FlowConfig();
        Assert.assertFalse(flowConfig.isInternalFlow());
        flowConfig.setName("__Internal__");
        Status status = flowConfig.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("name"));
        Assert.assertTrue(flowConfig.isInternalFlow());
        flowConfig.setName("External");
        Assert.assertFalse(flowConfig.isInternalFlow());
    }

    @Test
    public void testFlowConfigCreateSet() throws UnknownHostException {
        FlowConfig frmC = new FlowConfig();
        FlowConfig frmC3 = new FlowConfig();
        Node node = NodeCreator.createOFNode(1L);
        FlowEntry entry = new FlowEntry("polTest", null, getSampleFlowV6(node), node);

        // testing equal function
        Assert.assertFalse(frmC.equals(null));
        Assert.assertTrue(frmC.equals(frmC));
        Assert.assertTrue(frmC.equals(frmC3));
        Assert.assertFalse(frmC.equals(entry));
        FlowConfig flowC = createSampleFlowConfig();
        Assert.assertFalse(frmC.equals(flowC));
        // testing installInHW
        Assert.assertTrue(frmC.installInHw());
        frmC.setInstallInHw(false);
        Assert.assertFalse(frmC.installInHw());
        frmC.setInstallInHw(true);
        Assert.assertTrue(frmC.installInHw());

        // testing general set and get methods
        ArrayList<String> actions = createSampleActionList();
        frmC.setActions(actions);
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setActions(actions);

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setCookie("0");
        Assert.assertTrue(frmC.getCookie().equals("0"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setCookie("0");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setDstMac("00:A0:C9:22:AB:11");
        Assert.assertTrue(frmC.getDstMac().equals("00:A0:C9:22:AB:11"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setDstMac("00:A0:C9:22:AB:11");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setSrcMac("00:A0:C9:14:C8:29");
        Assert.assertTrue(frmC.getSrcMac().equals("00:A0:C9:14:C8:29"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setSrcMac("00:A0:C9:14:C8:29");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setDynamic(true);
        Assert.assertTrue(frmC.isDynamic());
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setDynamic(true);
        flowC.setDynamic(true);

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setEtherType("0x0800");
        Assert.assertTrue(frmC.getEtherType().equals("0x0800"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setEtherType("0x0800");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setIngressPort("60");
        Assert.assertTrue(frmC.getIngressPort().equals("60"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setIngressPort("60");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setName("Config1");
        Assert.assertTrue(frmC.getName().equals("Config1"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setName("Config1");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setDstIp("2.2.2.2");
        Assert.assertTrue(frmC.getDstIp().equals("2.2.2.2"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setDstIp("2.2.2.2");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setSrcIp("1.2.3.4");
        Assert.assertTrue(frmC.getSrcIp().equals("1.2.3.4"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setSrcIp("1.2.3.4");

        Assert.assertFalse(frmC.equals(flowC));
        Assert.assertFalse(frmC.isPortGroupEnabled());
        frmC.setPortGroup("2");
        Assert.assertTrue(frmC.isPortGroupEnabled());
        Assert.assertTrue(frmC.getPortGroup().equals("2"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setPortGroup("2");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setPriority("100");
        Assert.assertTrue(frmC.getPriority().equals("100"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setPriority("100");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setProtocol(IPProtocols.TCP.toString());
        Assert.assertTrue(frmC.getProtocol().equals(IPProtocols.TCP.toString()));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setProtocol(IPProtocols.TCP.toString());

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setNode(Node.fromString(Node.NodeIDType.OPENFLOW, "1"));
        Assert.assertTrue(frmC.getNode().equals(Node.fromString(Node.NodeIDType.OPENFLOW, "1")));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setNode(Node.fromString(Node.NodeIDType.OPENFLOW, "1"));

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setTosBits("0");
        Assert.assertTrue(frmC.getTosBits().equals("0"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setTosBits("0");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setDstPort("100");
        Assert.assertTrue(frmC.getDstPort().equals("100"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setDstPort("100");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setSrcPort("8080");
        Assert.assertTrue(frmC.getSrcPort().equals("8080"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setSrcPort("8080");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setVlanId("100");
        Assert.assertTrue(frmC.getVlanId().equals("100"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setVlanId("100");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setVlanPriority("0");
        Assert.assertTrue(frmC.getVlanPriority().equals("0"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setVlanPriority("0");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setIdleTimeout("300");
        Assert.assertTrue(frmC.getIdleTimeout().equals("300"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setIdleTimeout("300");

        Assert.assertFalse(frmC.equals(flowC));
        frmC.setHardTimeout("1000");
        Assert.assertTrue(frmC.getHardTimeout().equals("1000"));
        Assert.assertFalse(frmC.equals(frmC3));
        frmC3.setHardTimeout("1000");

        // Assert.assertFalse(frmC.equals(flowC));
        Assert.assertTrue(actions.equals(frmC.getActions()));

        FlowConfig frmC2 = new FlowConfig(frmC);

        Assert.assertFalse(frmC2.equals(frmC));
        frmC2.setDynamic(false);
        Assert.assertFalse(frmC2.equals(frmC));
        frmC2.setDynamic(true);
        Assert.assertTrue(frmC2.equals(frmC));
        // Assert.assertFalse(frmC2.equals(frmC3));
        flowC.setDynamic(true);
        Assert.assertTrue(flowC.equals(frmC));
        Assert.assertTrue(flowC.isStatusSuccessful());
        flowC.setStatus("Invalid");
        Assert.assertFalse(flowC.isStatusSuccessful());

        flowC.getActions().add(ActionType.DROP.toString());
        Assert.assertFalse(flowC.equals(frmC));
        Assert.assertFalse(flowC.isIPv6());
        flowC.setDstIp("2001:420:281:1004:407a:57f4:4d15:c355");
        Assert.assertTrue(flowC.isIPv6());
        flowC.setSrcIp("2001:420:281:1004:407a:57f4:4d15:c355");
        Assert.assertTrue(flowC.isIPv6());

        Long id = (Long) flowC.getNode().getID();
        Assert.assertTrue(id.toString().equals("1"));

    }

    @Test
    public void testFlowConfigEqualities() throws UnknownHostException {
        FlowConfig fc = new FlowConfig();
        FlowConfig fc2 = new FlowConfig();
        fc.setName("flow1");
        fc.setNode(Node.fromString(Node.NodeIDType.OPENFLOW, "1"));
        Assert.assertFalse(fc.onNode(Node.fromString(Node.NodeIDType.OPENFLOW, "0")));
        Assert.assertTrue(fc.onNode(Node.fromString(Node.NodeIDType.OPENFLOW, "1")));

        Assert.assertTrue(fc.isByNameAndNodeIdEqual("flow1", Node.fromString(Node.NodeIDType.OPENFLOW, "1")));
        Assert.assertFalse(fc.isByNameAndNodeIdEqual("flow1", Node.fromString(Node.NodeIDType.OPENFLOW, "0")));
        Assert.assertFalse(fc.isByNameAndNodeIdEqual("flow2", Node.fromString(Node.NodeIDType.OPENFLOW, "1")));

        Assert.assertFalse(fc.isByNameAndNodeIdEqual(fc2));
        fc2.setName("flow1");
        Assert.assertFalse(fc.isByNameAndNodeIdEqual(fc2));
        fc2.setNode(Node.fromString(Node.NodeIDType.OPENFLOW, "0"));
        Assert.assertFalse(fc.isByNameAndNodeIdEqual(fc2));
        fc2.setNode(Node.fromString(Node.NodeIDType.OPENFLOW, "1"));
        Assert.assertTrue(fc.isByNameAndNodeIdEqual(fc2));
    }

    @Test
    public void testStatusToggle() throws UnknownHostException {
        FlowConfig fc = new FlowConfig();
        fc.toggleInstallation();
        Assert.assertTrue(fc.installInHw());
        fc.toggleInstallation();
        Assert.assertFalse(fc.installInHw());
        fc.toggleInstallation();
        Assert.assertTrue(fc.installInHw());

    }

    @Test
    public void testGetFlowEntry() throws UnknownHostException {
        FlowConfig fc2 = createSampleFlowConfig();
        FlowEntry fe = fc2.getFlowEntry();
        Assert.assertNotNull(fe);
    }

    @Test
    public void testGetFlow() throws UnknownHostException {
        FlowConfig fc = new FlowConfig();
        fc.setActions(createSampleActionList());
        Flow flow = fc.getFlow();
        Assert.assertNotNull(flow);
    }

    @Test
    public void testL2AddressValid() {
        FlowConfig fc = new FlowConfig();
        Assert.assertFalse(fc.isL2AddressValid(null));
        Assert.assertFalse(fc.isL2AddressValid("11"));
        Assert.assertFalse(fc.isL2AddressValid("00:A0:C9:14:C8:"));
        Assert.assertFalse(fc.isL2AddressValid("000:A01:C9:14:C8:211"));

        Assert.assertTrue(fc.isL2AddressValid("00:A0:C9:14:C8:29"));
    }

    @Test
    public void testValid() throws UnknownHostException {
        FlowConfig fc2 = createSampleFlowConfig();
        Assert.assertTrue(fc2.validate(null).isSuccess());

        FlowConfig fc = new FlowConfig();
        Status status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Invalid name"));

        fc.setName("Config");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Node is null"));

        fc.setNode(Node.fromString(Node.NodeIDType.OPENFLOW, "1"));
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setPriority("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("is not in the range 0 - 65535"));

        fc.setPriority("100000");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("is not in the range 0 - 65535"));

        fc.setPriority("2000");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setCookie("100");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setIngressPort("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("is not valid for the Switch"));

        fc.setIngressPort("100");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setVlanId(("-1"));
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("is not in the range 0 - 4095"));

        fc.setVlanId("5000");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("is not in the range 0 - 4095"));

        fc.setVlanId("100");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setVlanPriority("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("is not in the range 0 - 7"));

        fc.setVlanPriority("9");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("is not in the range 0 - 7"));

        fc.setVlanPriority("5");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setEtherType("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Ethernet type"));

        fc.setEtherType("0xfffff");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Ethernet type"));

        fc.setEtherType("0x800");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setTosBits("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("IP ToS bits"));

        fc.setTosBits("65");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("IP ToS bits"));

        fc.setTosBits("60");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setSrcPort("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Transport source port"));

        fc.setSrcPort("0xfffff");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Transport source port"));

        fc.setSrcPort("0");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setSrcPort("0x00ff");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setSrcPort("0xffff");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setDstPort("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Transport destination port"));

        fc.setDstPort("0xfffff");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Transport destination port"));

        fc.setDstPort("0");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setDstPort("0x00ff");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setDstPort("0xffff");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setSrcMac("abc");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Ethernet source address"));

        fc.setSrcMac("00:A0:C9:14:C8:29");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setDstMac("abc");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Ethernet destination address"));

        fc.setDstMac("00:A0:C9:22:AB:11");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setSrcIp("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("IP source address"));

        fc.setSrcIp("2001:420:281:1004:407a:57f4:4d15:c355");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Type mismatch between Ethernet & Src IP"));

        fc.setEtherType("0x86dd");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setSrcIp("1.1.1.1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Type mismatch between Ethernet & Src IP"));

        fc.setEtherType("0x800");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setDstIp("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("IP destination address"));

        fc.setDstIp("2001:420:281:1004:407a:57f4:4d15:c355");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Type mismatch between Ethernet & Dst IP"));

        fc.setEtherType("0x86dd");
        fc.setSrcIp("2001:420:281:1004:407a:57f4:4d15:c355");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setDstIp("2.2.2.2");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Type mismatch between Ethernet & Dst IP"));

        fc.setEtherType("0x800");
        fc.setSrcIp("1.1.1.1");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setEtherType(null);
        fc.setSrcIp("2001:420:281:1004:407a:57f4:4d15:c355");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("IP Src Dest Type mismatch"));

        fc.setSrcIp("1.1.1.1");
        fc.setIdleTimeout("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Idle Timeout value"));

        fc.setIdleTimeout("0xfffff");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Idle Timeout value"));

        fc.setIdleTimeout("10");
        Assert.assertTrue(fc.validate(null).isSuccess());

        fc.setHardTimeout("-1");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Hard Timeout value"));

        fc.setHardTimeout("0xfffff");
        status = fc.validate(null);
        Assert.assertFalse(status.isSuccess());
        Assert.assertTrue(status.getDescription().contains("Hard Timeout value"));

        fc.setHardTimeout("10");
        Assert.assertTrue(fc.validate(null).isSuccess());

    }

    private FlowConfig createSampleFlowConfig() throws UnknownHostException {
        ArrayList<String> actions;
        actions = createSampleActionList();
        // actions.add(ActionType.CONTROLLER.toString());
        FlowConfig flowConfig = new FlowConfig("true", "Config1", Node.fromString(Node.NodeIDType.OPENFLOW, "1"),
                "100", "0", "60", "2", "100", "0", "0x0800", "00:A0:C9:14:C8:29", "00:A0:C9:22:AB:11",
                IPProtocols.TCP.toString(), "0", "1.2.3.4", "2.2.2.2", "8080", "100", "300", "1000", actions);
        return flowConfig;

    }

    private ArrayList<String> createSampleActionList() {
        ArrayList<String> actions = new ArrayList<String>();
        actions.add(ActionType.DROP.toString());
        actions.add(ActionType.LOOPBACK.toString());
        actions.add(ActionType.FLOOD.toString());
        actions.add(ActionType.SW_PATH.toString());
        actions.add(ActionType.HW_PATH.toString());
        actions.add(ActionType.SET_VLAN_PCP.toString() + "=1");
        actions.add(ActionType.SET_VLAN_ID.toString() + "=1");
        actions.add(ActionType.POP_VLAN.toString());
        actions.add(ActionType.SET_DL_SRC.toString() + "=00:A0:C1:AB:22:11");
        actions.add(ActionType.SET_DL_DST.toString() + "=00:B1:C1:00:AA:BB");
        actions.add(ActionType.SET_NW_SRC.toString() + "=1.1.1.1");
        actions.add(ActionType.SET_NW_DST.toString() + "=2.2.2.2");
        actions.add(ActionType.CONTROLLER.toString());
        actions.add(ActionType.SET_NW_TOS.toString() + "1");
        actions.add(ActionType.SET_TP_SRC.toString() + "60");
        actions.add(ActionType.SET_TP_DST.toString() + "8080");
        actions.add(ActionType.SET_NEXT_HOP.toString() + "=1.1.1.1");

        return actions;
    }

    private Flow getSampleFlowV6(Node node) throws UnknownHostException {
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector((short) 24, node);
        NodeConnector oport = NodeConnectorCreator.createOFNodeConnector((short) 30, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        byte newMac[] = { (byte) 0x11, (byte) 0xaa, (byte) 0xbb, (byte) 0x34, (byte) 0x9a, (byte) 0xee };
        InetAddress srcIP = InetAddress.getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress.getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMask = InetAddress.getByName("ffff:ffff:ffff:ffff:0:0:0:0");
        InetAddress ipMask2 = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        InetAddress newIP = InetAddress.getByName("2056:650::a1b0");
        short ethertype = EtherTypes.IPv6.shortValue();
        short vlan = (short) 27;
        byte vlanPr = (byte) 3;
        Byte tos = 4;
        byte proto = IPProtocols.UDP.byteValue();
        short src = (short) 5500;
        short dst = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match = new Match();
        match.setField(MatchType.IN_PORT, port);
        match.setField(MatchType.DL_SRC, srcMac);
        match.setField(MatchType.DL_DST, dstMac);
        match.setField(MatchType.DL_TYPE, ethertype);
        match.setField(MatchType.DL_VLAN, vlan);
        match.setField(MatchType.DL_VLAN_PR, vlanPr);
        match.setField(MatchType.NW_SRC, srcIP, ipMask);
        match.setField(MatchType.NW_DST, dstIP, ipMask2);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src);
        match.setField(MatchType.TP_DST, dst);

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Controller());
        actions.add(new SetVlanId(5));
        actions.add(new SetDlDst(newMac));
        actions.add(new SetNwDst(newIP));
        actions.add(new Output(oport));
        actions.add(new PopVlan());
        actions.add(new Flood());

        Flow flow = new Flow(match, actions);
        flow.setPriority((short) 300);
        flow.setHardTimeout((short) 240);

        return flow;
    }
}
