/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.Diff;
import org.junit.Assert;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author michal.rehak
 *
 */
public abstract class NodeHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeHelper.class);
    
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
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "element-id"), null, "ntElementId_09"));
        CompositeNode ntElementNode1 = NodeFactory.createImmutableCompositeNode(new QName(qName, "network-element"), null, value);
        assignParentToChildren(ntElementNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "element-id"), null, "ntElementId_10"));
        CompositeNode ntElementNode2 = NodeFactory.createImmutableCompositeNode(new QName(qName, "network-element"), null, value);
        assignParentToChildren(ntElementNode2);
        
        value = new ArrayList<Node<?>>();
        value.add(ntElementNode1);
        value.add(ntElementNode2);
        CompositeNode ntElementsNode = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "network-elements"), null, value);
        assignParentToChildren(ntElementsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "dest-node"), null, "nodeId_07"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "dest-tp"), null, "tpId_08"));
        CompositeNode destination = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "destination"), null, value);
        assignParentToChildren(destination);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "source-node"), null, "nodeId_05"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "source-tp"), null, "tpId_06"));
        CompositeNode source = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "source"), null, value);
        assignParentToChildren(source);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "link-id"), null, "linkId_04"));
        value.add(source);
        value.add(destination);
        CompositeNode link1 = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "link"), null, value);
        assignParentToChildren(link1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "dest-node"), null, "nodeId_14"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "dest-tp"), null, "tpId_15"));
        destination = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "destination"), null, value);
        assignParentToChildren(destination);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "source-node"), null, "nodeId_12"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "source-tp"), null, "tpId_13"));
        source = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "source"), null, value);
        assignParentToChildren(source);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "link-id"), null, "linkId_11"));
        value.add(source);
        value.add(destination);
        CompositeNode link2 = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "link"), null, value);
        assignParentToChildren(link2);
        
        value = new ArrayList<Node<?>>(); 
        value.add(link1);
        value.add(link2);
        CompositeNode links = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "links"), null, value);
        assignParentToChildren(links);
        
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "tp-id"), null, "tpId_03"));
        CompositeNode terminationPointNode1 = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(terminationPointNode1);
        CompositeNode terminationPointsNode = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "termination-points"), null, value);
        assignParentToChildren(terminationPointsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "node-id"), null, "nodeId_02"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "supporting-ne"), null, "networkId_02"));
        value.add(terminationPointsNode);
        CompositeNode node1Node = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "node"), null, value);
        assignParentToChildren(node1Node);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "tp-id"), null, "tpId_18"));
        terminationPointNode1 = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(terminationPointNode1);
        terminationPointsNode = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "termination-points"), null, value);
        assignParentToChildren(terminationPointsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "node-id"), null, "nodeId_16"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "supporting-ne"), null, "networkId_17"));
        value.add(terminationPointsNode);
        CompositeNode node2Node = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "node"), null, value);
        assignParentToChildren(node2Node);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "tp-id"), null, "tpId_18"));
        terminationPointNode1 = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode1);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "tp-id"), null, "tpId_19"));
        CompositeNode terminationPointNode2 = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "termination-point"), null, value);
        assignParentToChildren(terminationPointNode2);
        
        value = new ArrayList<Node<?>>(); 
        value.add(terminationPointNode1);
        value.add(terminationPointNode2);
        terminationPointsNode = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "termination-points"), null, value);
        assignParentToChildren(terminationPointsNode);
        
        value = new ArrayList<Node<?>>(); 
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "node-id"), null, "nodeId_19"));
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "supporting-ne"), null, "networkId_20"));
        value.add(terminationPointsNode);
        CompositeNode node3Node = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "node"), null, value);
        assignParentToChildren(node3Node);
        
        value = new ArrayList<Node<?>>(); 
        value.add(node1Node);
        value.add(node2Node);
        value.add(node3Node);
        CompositeNode nodesNode = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "nodes"), null, value);
        assignParentToChildren(nodesNode);
        
        value = new ArrayList<Node<?>>();
        value.add(links);
        value.add(nodesNode);
        value.add(NodeFactory.createImmutableSimpleNode(new QName(qName, "topology-id"), null, "topId_01"));
        CompositeNode topology = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "topology"), null, value);
        assignParentToChildren(topology);
        
        value = new ArrayList<Node<?>>();
        value.add(topology);
        CompositeNode topologies = NodeFactory.createImmutableCompositeNode(
                new QName(qName, "topologies"), null, value);
        assignParentToChildren(topologies);
        
        value = new ArrayList<Node<?>>();
        value.add(topologies);
        value.add(ntElementsNode);
        CompositeNode network = NodeFactory.createImmutableCompositeNode(
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
    
    /**
     * @param scriptName 
     * @return tree root
     * @throws Exception
     */
    public static CompositeNode loadConfigByGroovy(String scriptName) throws Exception {
    	InputStream configStream = NodeHelper.class.getResourceAsStream(scriptName);
    	Binding binding = new Binding();
    	GroovyShell gShell = new GroovyShell(binding);
    	LOG.debug("groovy: starting script parse..  " + scriptName);
		Script configScript = gShell.parse(new InputStreamReader(configStream));
		LOG.debug("groovy: starting script..  " + scriptName);
		configScript.run();
		LOG.debug("groovy: digging result");
    	Object xmlGen = binding.getVariable("xmlGen");
    	LOG.debug("xmlGen = " + xmlGen);
    	Method getter = xmlGen.getClass().getDeclaredMethod("getBuilder", new Class[0]);
    	MyNodeBuilder builder = (MyNodeBuilder) getter.invoke(xmlGen, new Object[0]);
    	
    	return builder.getRootNode();
    }
    
    /**
     * @param pattern , e.g.: <pre>"//{0}:network/{1}:xx[text() = 'sss']"</pre>
     * @param nsArg , e.g.: <pre>{"uri:ns1", "uri:ns2"}</pre>
     * @return pattern with namespaces: <pre>//uri:ns1:network/uri:ns2:xx[text() = ''sss'']"</pre>
     */
    public static String AddNamespaceToPattern(String pattern, Object... nsArg) {
        Object[] ns = nsArg;
        String patternNs = pattern.replaceAll("'", "''");
        if (ns == null) {
            ns = new Object[]{""};
        } else {
            // add ':' into pattern after placeholders
            patternNs = patternNs.replaceAll("(\\{[0-9]+\\})", "$1:");
        }
        
        return MessageFormat.format(patternNs, ns);
    }

    /**
     * @param tree
     * @param xmlFile 
     * @param clazz 
     * @throws Exception
     * @throws SAXException
     * @throws IOException
     */
    public static void compareXmlTree(Document tree, String xmlFile, Class<?> clazz) throws Exception,
            SAXException, IOException {
        ByteArrayOutputStream actualRaw = new ByteArrayOutputStream();
        dumpDoc(tree, new PrintStream(actualRaw));
        Reader actualReader = new InputStreamReader(new ByteArrayInputStream(actualRaw.toByteArray()));
        
        Reader expectedReader = new InputStreamReader(clazz.getResourceAsStream(xmlFile));
        Diff myDiff = new Diff(expectedReader, actualReader);
        myDiff.overrideDifferenceListener(new IgnoreWhiteCharsDiffListener());
        
        boolean similar = myDiff.similar();
        if (! similar) {
            System.out.println(new String(actualRaw.toByteArray()));
        }
        Assert.assertEquals(myDiff.toString(), true, similar);
    }

}
