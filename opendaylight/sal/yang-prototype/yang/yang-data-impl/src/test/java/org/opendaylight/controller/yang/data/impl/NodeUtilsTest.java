/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @author michal.rehak
 *
 */
public class NodeUtilsTest {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(NodeUtilsTest.class);
    
    private QName qName;
    private CompositeNode network;

    private String ns;


    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        ns = "urn:ietf:params:xml:ns:netconf:base:1.0";
        qName = new QName(
                new URI(ns), 
                new Date(42), "yang-data-impl-mutableTest");
        network = NodeHelper.buildTestConfigTree(qName);
    }

    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.NodeUtils#buildPath(org.opendaylight.controller.yang.data.api.Node)}.
     * @throws Exception 
     */
    @Test
    public void testBuildPath() throws Exception {
        SimpleNode<?> needle = network.getCompositesByName("topologies").iterator().next()
            .getCompositesByName("topology").iterator().next()
            .getSimpleNodesByName("topology-id").iterator().next();
        String breadCrumbs = NodeUtils.buildPath(needle);
        
        Assert.assertEquals("network.topologies.topology.topology-id", breadCrumbs);
    }

    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.NodeUtils#buildShadowDomTree(org.opendaylight.controller.yang.data.api.CompositeNode)}.
     * @throws Exception 
     */
    @Test
    public void testBuildShadowDomTree() throws Exception {
        MemoryConsumption mc = new MemoryConsumption();
        mc.startObserving();
        
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        
        LOG.debug("After dom built: "+mc.finishObserving());
        NodeHelper.compareXmlTree(networkShadow, "./config02-shadow.xml", getClass());
    }

    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.NodeUtils#findNodeByXpath(org.w3c.dom.Document, java.lang.String)}.
     * @throws Exception 
     */
    @Test
    public void testFindNodeByXpath() throws Exception {
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        MemoryConsumption mc = new MemoryConsumption();
        mc.startObserving();
        
        SimpleNode<String> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                        "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));
        
        LOG.debug("After xpath executed: "+mc.finishObserving());
        
        Assert.assertNotNull(needle);
        Assert.assertEquals("tpId_18", needle.getValue());
    }
    
    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.NodeUtils#buildNodeMap(java.util.List)}.
     */
    @Test
    public void testBuildNodeMap() {
        CompositeNode topology = network.getCompositesByName("topologies").iterator().next()
            .getCompositesByName("topology").iterator().next();
        
        Map<QName, List<Node<?>>> nodeMap = NodeUtils.buildNodeMap(topology.getChildren());
        Assert.assertEquals(3, nodeMap.size());
    }
    
    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.NodeUtils#buildMapOfListNodes(org.opendaylight.controller.yang.model.api.SchemaContext)}.
     */
    @Test
    public void testBuildMapOfListNodes() {
        SchemaContext schemaCtx = NodeHelper.loadSchemaContext();
        Map<String, ListSchemaNode> mapOfLists = NodeUtils.buildMapOfListNodes(schemaCtx);
        Assert.assertEquals(5, mapOfLists.size());
    }

    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.NodeUtils#buildMapOfListNodes(org.opendaylight.controller.yang.model.api.SchemaContext)}.
     * @throws Exception 
     * @throws IOException 
     */
    @Test
    public void testLoadConfigByGroovy() throws IOException, Exception {
    	CompositeNode treeRoot = NodeHelper.loadConfigByGroovy("./config02.groovy");
    	Document shadowTree = NodeUtils.buildShadowDomTree(treeRoot);
    	try {
            checkFamilyBinding(treeRoot);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw e;
        }
    	
    	NodeHelper.compareXmlTree(shadowTree, "./config02g-shadow.xml", getClass());
    }

    private static void checkFamilyBinding(CompositeNode treeRoot) throws Exception {
        Stack<CompositeNode> jobQueue = new Stack<>();
        jobQueue.push(treeRoot);
        
        while (!jobQueue.isEmpty()) {
            CompositeNode job = jobQueue.pop();
            for (Node<?> child : job.getChildren()) {
                if (child instanceof CompositeNode) {
                    jobQueue.push((CompositeNode) child);
                }
                
                if (job != child.getParent()) {
                    throw new Exception("binding mismatch occured: \nPARENT["+job+"]\n CHILD[" + child+"]\n  +->  "+child.getParent());
                }
            }
        }
    }
    
}
