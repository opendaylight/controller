/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;
import org.w3c.dom.Document;

/**
 * @author michal.rehak
 *
 */
public abstract class NodeHelper {
    
    /** xml source of example network configuration */
    public static final String NETWORK_XML = 
      "<network xmlns=\"urn:opendaylight:controller:network\">\n" +
      //"<network>\n" +
      "    <topologies>\n" +
      "        <topology>\n" +
      "            <topology-id>topId_01</topology-id>\n" +
      "            <nodes>\n" +
      "                <node>\n" +
      "                    <node-id>nodeId_02</node-id>\n" +
      "                    <supporting-ne>networkId_02</supporting-ne>\n" +
      "                    <termination-points>\n" +
      "                        <termination-point>\n" +
      "                            <tp-id>tpId_03</tp-id>\n" +
      "                        </termination-point>\n" +
      "                    </termination-points>\n" +
      "                </node>\n" +
      "                <node>\n" +
      "                    <node-id>nodeId_16</node-id>\n" +
      "                    <supporting-ne>networkId_17</supporting-ne>\n" +
      "                    <termination-points>\n" +
      "                        <termination-point>\n" +
      "                            <tp-id>tpId_18</tp-id>\n" +
      "                        </termination-point>\n" +
      "                    </termination-points>\n" +
      "                </node>\n" +
      "                <node>\n" +
      "                    <node-id>nodeId_19</node-id>\n" +
      "                    <supporting-ne>networkId_20</supporting-ne>\n" +
      "                    <termination-points>\n" +
      "                        <termination-point>\n" +
      "                            <tp-id>tpId_18</tp-id>\n" +
      "                        </termination-point>\n" +
      "                        <termination-point>\n" +
      "                            <tp-id>tpId_19</tp-id>\n" +
      "                        </termination-point>\n" +
      "                    </termination-points>\n" +
      "                </node>\n" +
      "            </nodes>\n" +
      "            <links>\n" +
      "                <link>\n" +
      "                    <link-id>linkId_04</link-id>\n" +
      "                    <source>\n" +
      "                        <source-node>nodeId_05</source-node>\n" +
      "                        <source-tp>tpId_06</source-tp>\n" +
      "                    </source>\n" +
      "                    <destination>\n" +
      "                        <dest-node>nodeId_07</dest-node>\n" +
      "                        <dest-tp>tpId_08</dest-tp>\n" +
      "                    </destination>\n" +
      "                </link>\n" +
      "                <link>\n" +
      "                    <link-id>linkId_11</link-id>\n" +
      "                    <source>\n" +
      "                        <source-node>nodeId_12</source-node>\n" +
      "                        <source-tp>tpId_13</source-tp>\n" +
      "                    </source>\n" +
      "                    <destination>\n" +
      "                        <dest-node>nodeId_14</dest-node>\n" +
      "                        <dest-tp>tpId_15</dest-tp>\n" +
      "                    </destination>\n" +
      "                </link>\n" +
      "            </links>\n" +
      "        </topology>\n" +
      "    </topologies>\n" +
      "    <network-elements>\n" +
      "        <network-element>\n" +
      "            <element-id>ntElementId_09</element-id>\n" +
      "        </network-element>\n" +
      "        <network-element>\n" +
      "            <element-id>ntElementId_10</element-id>\n" +
      "        </network-element>\n" +
      "    </network-elements>\n" +
      "</network>";

    /**
     * @param domTree
     * @param out
     * @throws Exception 
     */
    public static void dumpDoc(Document domTree, PrintStream out) throws Exception {
      TransformerFactory transformerFact = TransformerFactory.newInstance();
      transformerFact.setAttribute("indent-number", 4);
      Transformer transformer = transformerFact.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      //initialize StreamResult with File object to save to file
      StreamResult result = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(domTree);
      transformer.transform(source, result);
      String xmlString = result.getWriter().toString();
      out.println(xmlString);
    }

    /**
     * @param qName
     * @return example tree, see {@link #NETWORK_XML}
     */
    public static CompositeNode buildTestConfigTree(QName qName) {
        List<Node<?>> value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "element-id"), null, "ntElementId_09"));
        CompositeNode ntElementNode1 = NodeFactory.createCompositeNode(new QName(qName, "network-element"), null, value);
        assignParentToChildren(ntElementNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "element-id"), null, "ntElementId_10"));
        CompositeNode ntElementNode2 = NodeFactory.createCompositeNode(new QName(qName, "network-element"), null, value);
        assignParentToChildren(ntElementNode2);
        
        value = new ArrayList<Node<?>>();
        value.add(ntElementNode1);
        value.add(ntElementNode2);
        CompositeNode ntElementsNode = NodeFactory.createCompositeNode(
                new QName(qName, "network-elements"), null, value);
        assignParentToChildren(ntElementsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "dest-node"), null, "nodeId_07"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "dest-tp"), null, "tpId_08"));
        CompositeNode destination = NodeFactory.createCompositeNode(
                new QName(qName, "destination"), null, value);
        assignParentToChildren(destination);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "source-node"), null, "nodeId_05"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "source-tp"), null, "tpId_06"));
        CompositeNode source = NodeFactory.createCompositeNode(
                new QName(qName, "source"), null, value);
        assignParentToChildren(source);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "link-id"), null, "linkId_04"));
        value.add(source);
        value.add(destination);
        CompositeNode link1 = NodeFactory.createCompositeNode(
                new QName(qName, "link"), null, value);
        assignParentToChildren(link1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "dest-node"), null, "nodeId_14"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "dest-tp"), null, "tpId_15"));
        destination = NodeFactory.createCompositeNode(
                new QName(qName, "destination"), null, value);
        assignParentToChildren(destination);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "source-node"), null, "nodeId_12"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "source-tp"), null, "tpId_13"));
        source = NodeFactory.createCompositeNode(
                new QName(qName, "source"), null, value);
        assignParentToChildren(source);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "link-id"), null, "linkId_11"));
        value.add(source);
        value.add(destination);
        CompositeNode link2 = NodeFactory.createCompositeNode(
                new QName(qName, "link"), null, value);
        assignParentToChildren(link2);
        
        value = new ArrayList<Node<?>>(); 
        value.add(link1);
        value.add(link2);
        CompositeNode links = NodeFactory.createCompositeNode(
                new QName(qName, "links"), null, value);
        assignParentToChildren(links);
        
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "tp-id"), null, "tpId_03"));
        CompositeNode terminationPointNode1 = NodeFactory.createCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(terminationPointNode1);
        CompositeNode terminationPointsNode = NodeFactory.createCompositeNode(
                new QName(qName, "termination-points"), null, value);
        assignParentToChildren(terminationPointsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "node-id"), null, "nodeId_02"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "supporting-ne"), null, "networkId_02"));
        value.add(terminationPointsNode);
        CompositeNode node1Node = NodeFactory.createCompositeNode(
                new QName(qName, "node"), null, value);
        assignParentToChildren(node1Node);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "tp-id"), null, "tpId_18"));
        terminationPointNode1 = NodeFactory.createCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(terminationPointNode1);
        terminationPointsNode = NodeFactory.createCompositeNode(
                new QName(qName, "termination-points"), null, value);
        assignParentToChildren(terminationPointsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "node-id"), null, "nodeId_16"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "supporting-ne"), null, "networkId_17"));
        value.add(terminationPointsNode);
        CompositeNode node2Node = NodeFactory.createCompositeNode(
                new QName(qName, "node"), null, value);
        assignParentToChildren(node2Node);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "tp-id"), null, "tpId_18"));
        terminationPointNode1 = NodeFactory.createCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "tp-id"), null, "tpId_19"));
        CompositeNode terminationPointNode2 = NodeFactory.createCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode2);
        
        value = new ArrayList<Node<?>>(); 
        value.add(terminationPointNode1);
        value.add(terminationPointNode2);
        terminationPointsNode = NodeFactory.createCompositeNode(
                new QName(qName, "termination-points"), null, value);
        assignParentToChildren(terminationPointsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createSimpleNode(new QName(qName, "node-id"), null, "nodeId_19"));
        value.add(NodeFactory.createSimpleNode(new QName(qName, "supporting-ne"), null, "networkId_20"));
        value.add(terminationPointsNode);
        CompositeNode node3Node = NodeFactory.createCompositeNode(
                new QName(qName, "node"), null, value);
        assignParentToChildren(node3Node);
        
        value = new ArrayList<Node<?>>(); 
        value.add(node1Node);
        value.add(node2Node);
        value.add(node3Node);
        CompositeNode nodesNode = NodeFactory.createCompositeNode(
                new QName(qName, "nodes"), null, value);
        assignParentToChildren(nodesNode);
        
        value = new ArrayList<Node<?>>();
        value.add(links);
        value.add(nodesNode);
        value.add(NodeFactory.createSimpleNode(new QName(qName, "topology-id"), null, "topId_01"));
        CompositeNode topology = NodeFactory.createCompositeNode(
                new QName(qName, "topology"), null, value);
        assignParentToChildren(topology);
        
        value = new ArrayList<Node<?>>();
        value.add(topology);
        CompositeNode topologies = NodeFactory.createCompositeNode(
                new QName(qName, "topologies"), null, value);
        assignParentToChildren(topologies);
        
        value = new ArrayList<Node<?>>();
        value.add(topologies);
        CompositeNode network = NodeFactory.createCompositeNode(
                new QName(qName, "network"), null, value);
        assignParentToChildren(network);
        
        return network;
    }

    /**
     * @param parentNode
     */
    public static void assignParentToChildren(CompositeNode parentNode) {
        for (Node<?> child : parentNode.getChildren()) {
            ((AbstractNodeTO<?>) child).setParent(parentNode);
        }
    }

    /**
     * @return schema context of controller-network.yang
     */
    public static SchemaContext loadSchemaContext() {
        YangModelParser yParser = new YangParserImpl();
        List<InputStream> yangInputStreams = new ArrayList<>();
        yangInputStreams.add(NodeHelper.class.getResourceAsStream(
                "/controller-network.yang"));
        yangInputStreams.add(NodeHelper.class.getResourceAsStream(
                "/ietf-inet-types@2010-09-24.yang"));
        Set<Module> modules = yParser
                .parseYangModelsFromStreams(yangInputStreams);
        return yParser.resolveSchemaContext(modules);
    }

}
