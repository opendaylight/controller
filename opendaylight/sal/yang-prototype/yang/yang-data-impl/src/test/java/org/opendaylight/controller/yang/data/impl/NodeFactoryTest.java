/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModification;
import org.w3c.dom.Document;

/**
 * @author michal.rehak
 * 
 */
public class NodeFactoryTest {

    /**
     * Test method for methods creating immutable nodes in
     * {@link org.opendaylight.controller.yang.data.impl.NodeFactory}.
     * @throws Exception 
     */
    @Test
    public void testImmutableNodes() throws Exception {
        QName qName = new QName(
                new URI("urn:opendaylight:controller:network"), 
                new Date(42), "yang-data-impl-immutableTest_", null);
        
        CompositeNode network = NodeHelper.buildTestConfigTree(qName);
        
        
        Assert.assertEquals(1, network.getChildren().size());
        Document domTree = NodeUtils.buildShadowDomTree(network);
        NodeHelper.dumpDoc(domTree, System.out);
        
        CompositeNode tpList = NodeUtils.findNodeByXpath(domTree, 
                "//node[node-id/text()='nodeId_19']/termination-points");
        
        
        Assert.assertEquals(2, tpList.getCompositesByName("termination-point").size());
//        Assert.assertEquals(1, topologies.getCompositesByName("topology").size());
//        Assert.assertEquals(2, destination.getChildren().size());
    }

    /**
     * Test method for methods creating immutable nodes in
     * {@link org.opendaylight.controller.yang.data.impl.NodeFactory}.
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
        
        QName qName = new QName(
                new URI("urn:ietf:params:xml:ns:netconf:base:1.0"), 
                new Date(42), "yang-data-impl-mutableTest");
        
        List<Node<?>> value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "name"), null, "Ethernet0/0"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "mtu"), null, 1500));
        
        CompositeNodeModificationTOImpl ifNode = NodeFactory.createCompositeNodeModification(
                new QName(qName, "interface"), null, value, ModifyAction.DELETE);
        NodeHelper.assignParentToChildren(ifNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "name"), null, "Ethernet1/0"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "mtu"), null, 1501));
        
        CompositeNode ifNode2 = NodeFactory.createCompositeNode(new QName(qName, "interface"), null, value);
        NodeHelper.assignParentToChildren(ifNode2);

        value = new ArrayList<Node<?>>(); 
        value.add(ifNode);
        value.add(ifNode2);
        
        CompositeNode topNode = NodeFactory.createCompositeNode(new QName(qName, "top"), null, value);
        NodeHelper.assignParentToChildren(topNode);
        value = new ArrayList<Node<?>>(); 
        value.add(topNode);
        
        CompositeNode root = NodeFactory.createCompositeNode(new QName(qName, "config"), null, value);
        
        
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertEquals(1, ifNode.getSimpleNodesByName("name").size());
        Assert.assertEquals(1, ifNode.getSimpleNodesByName("mtu").size());
        Assert.assertEquals(2, topNode.getCompositesByName("interface").size());
        NodeModification interfaceMod = (NodeModification) 
                topNode.getCompositesByName("interface").get(0);
        Assert.assertEquals(ModifyAction.DELETE, interfaceMod.getModificationAction());
    }

    /**
     * test modifications builder
     * @throws Exception 
     */
    @Test
    public void testCopyDeepNode() throws Exception {
        QName qName = new QName(
                new URI("urn:opendaylight:controller:network"), 
                new Date(42), "yang-data-impl-immutableTest_", null);
        
        CompositeNode network = NodeHelper.buildTestConfigTree(qName);
        Map<Node<?>, Node<?>> mutableToOrig = new HashMap<>();
        MutableCompositeNode mutableNetwork = NodeFactory.copyDeepNode(network, mutableToOrig );

        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        NodeHelper.dumpDoc(networkShadow, new PrintStream(expected));
        
        Document mutableNetworkShadow = NodeUtils.buildShadowDomTree(mutableNetwork);
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        NodeHelper.dumpDoc(mutableNetworkShadow, new PrintStream(actual));
        
        Assert.assertEquals(new String(expected.toByteArray()), new String(actual.toByteArray()));
    }

}
