/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModification;
import org.opendaylight.controller.yang.data.api.SimpleNode;
import org.w3c.dom.Document;

/**
 * @author michal.rehak
 * 
 */
public class NodeFactoryTest {
    
    private QName qName;
    private CompositeNode network;

    private String ns;
    private Document networkShadow;


    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        ns = "urn:ietf:params:xml:ns:netconf:base:1.0";
        qName = new QName(
                new URI(ns), 
                new Date(42), null);
        network = NodeHelper.buildTestConfigTree(qName);
        networkShadow = NodeUtils.buildShadowDomTree(network);
        NodeHelper.compareXmlTree(networkShadow, "./config02-shadow.xml", getClass());
    }

    /**
     * Test method for methods creating immutable nodes in
     * {@link org.opendaylight.controller.yang.data.impl.NodeFactory}.
     * @throws Exception 
     */
    @Test
    public void testImmutableNodes() throws Exception {
        Assert.assertEquals(2, network.getChildren().size());
        CompositeNode tpList = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                        "//{0}node[{0}node-id/text()='nodeId_19']/{0}termination-points", ns));
        
        
        Assert.assertEquals(2, tpList.getCompositesByName("termination-point").size());
    }

    /**
     * Test method for methods creating immutable and mutable nodes:
     * {@link NodeFactory#createMutableCompositeNode(QName, CompositeNode, List, ModifyAction, CompositeNode)},
     * {@link NodeFactory#createMutableSimpleNode(QName, CompositeNode, Object, ModifyAction, SimpleNode)}
     * @throws Exception 
     */
    @Test
    public void testMutableNodes() throws Exception {
        // <config>
        //   <top xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
        //     <interface xc:operation="delete">
        //       <name>Ethernet0/0</name>
        //       <mtu>1500</mtu>
        //     </interface>
        //     <interface>
        //       <name>Ethernet0/1</name>
        //       <mtu>1501</mtu>
        //     </interface>
        //   </top>
        // </config>
        
        
        List<Node<?>> value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "name"), null, "Ethernet0/0"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "mtu"), null, 1500));
        
        MutableCompositeNode ifNode = NodeFactory.createMutableCompositeNode(
                new QName(qName, "interface"), null, value, ModifyAction.DELETE, null);
        ifNode.init();
        NodeHelper.assignParentToChildren(ifNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "name"), null, "Ethernet1/0"));
        value.add(NodeFactory.createMutableSimpleNode(new QName(qName, "mtu"), null, 1501, ModifyAction.REMOVE, null));
        
        CompositeNode ifNode2 = NodeFactory.createImmutableCompositeNode(new QName(qName, "interface"), null, value);
        NodeHelper.assignParentToChildren(ifNode2);

        value = new ArrayList<Node<?>>(); 
        value.add(ifNode);
        value.add(ifNode2);
        
        CompositeNode topNode = NodeFactory.createImmutableCompositeNode(new QName(qName, "top"), null, value);
        NodeHelper.assignParentToChildren(topNode);
        value = new ArrayList<Node<?>>(); 
        value.add(topNode);
        
        CompositeNode root = NodeFactory.createImmutableCompositeNode(new QName(qName, "config"), null, value);
        Document shadowConfig = NodeUtils.buildShadowDomTree(root);
        NodeHelper.compareXmlTree(shadowConfig, "./mutableNodesConfig.xml", getClass());
        
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertEquals(1, ifNode.getSimpleNodesByName("name").size());
        Assert.assertEquals(1, ifNode.getSimpleNodesByName("mtu").size());
        Assert.assertEquals(2, topNode.getCompositesByName("interface").size());
        NodeModification interfaceMod = topNode.getCompositesByName("interface").get(0);
        Assert.assertEquals(ModifyAction.DELETE, interfaceMod.getModificationAction());
    }

    /**
     * test of {@link NodeFactory#copyDeepAsMutable(CompositeNode, Map)}
     * @throws Exception 
     */
    @Test
    public void testCopyDeepAsMutable() throws Exception {
        Map<Node<?>, Node<?>> mutableToOrig = new HashMap<>();
        CompositeNode mutableNetwork = NodeFactory.copyDeepAsMutable(network, mutableToOrig);

        Document mutableNetworkShadow = NodeUtils.buildShadowDomTree(mutableNetwork);
        
        NodeHelper.compareXmlTree(mutableNetworkShadow, "./config02-shadow.xml", getClass());
        
        CompositeNode immutableNetwork = NodeFactory.copyDeepAsImmutable(mutableNetwork, null);
        Assert.assertEquals(network, immutableNetwork);
    }
    
    
    /**
     * test of {@link NodeFactory#copyDeepAsImmutable(CompositeNode, Map)}
     * @throws Exception 
     */
    @Test
    public void testCopyDeepAsImmutable() throws Exception {
        Map<Node<?>, Node<?>> mutableToOrig = new HashMap<>();
        CompositeNode immutableNetwork = NodeFactory.copyDeepAsImmutable(network, mutableToOrig);
        
        Document mutableNetworkShadow = NodeUtils.buildShadowDomTree(immutableNetwork);
        NodeHelper.compareXmlTree(mutableNetworkShadow, "./config02-shadow.xml", getClass());
        
        Assert.assertEquals(network, immutableNetwork);
    }

}
