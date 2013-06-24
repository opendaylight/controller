/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.net.URI;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @author michal.rehak
 * 
 */
public class NodeModificationBuilderImplTest {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(NodeModificationBuilderImplTest.class);

    private SchemaContext schemaCtx;
    private QName qName;
    private CompositeNode network;
    private NodeModificationBuilderImpl nodeModificationBuilder;

    private String ns;

    /**
     * @throws Exception
     */
    private void dumpResult() throws Exception {
        CompositeNode diffTree = nodeModificationBuilder.buildDiffTree();
        CompositeNode diffTreeImmutable = NodeFactory.copyDeepAsImmutable(diffTree, null);
        
        Document diffShadow = NodeUtils.buildShadowDomTree(diffTreeImmutable);
        NodeHelper.dumpDoc(diffShadow, System.out);
    }

    /**
     * prepare schemaContext
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        schemaCtx = NodeHelper.loadSchemaContext();

        ns = "urn:opendaylight:controller:network";
        qName = new QName(
                new URI(ns), 
                new Date(1369000800000L), "topos");
        network = NodeHelper.buildTestConfigTree(qName);
        
        nodeModificationBuilder = new NodeModificationBuilderImpl(network, schemaCtx);
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#getMutableEquivalent(org.opendaylight.controller.yang.data.api.Node)}
     * .
     */
    @Test
    public void testGetMutableEquivalent() {
        MutableCompositeNode rootMutable = (MutableCompositeNode) 
                nodeModificationBuilder.getMutableEquivalent(network);
        
        CompositeNode topologies = network.getCompositesByName("topologies").iterator().next();
        Node<?> mutableEquivalent = nodeModificationBuilder.getMutableEquivalent(topologies);
        CompositeNode topologiesMutable = rootMutable.getCompositesByName("topologies").iterator().next();
        
        Assert.assertSame(topologiesMutable, mutableEquivalent);
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeAddSimple() throws Exception {
        LOG.debug("testBuildDiffTreeAddSimple");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        CompositeNode needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]", ns));
        
        MutableCompositeNode mutableParent = (MutableCompositeNode) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        MutableSimpleNode<String> newMutable = NodeFactory.createMutableSimpleNode(
                new QName(needle.getNodeType(), "anySubNode"), mutableParent, "42", null, null);
        
        nodeModificationBuilder.addNode(newMutable);
        dumpResult();
    }
    
    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeAddComposite() throws Exception {
        LOG.debug("testBuildDiffTreeAddComposite");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        CompositeNode needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]", ns));
        
        MutableCompositeNode mutableParent = (MutableCompositeNode) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        MutableSimpleNode<String> newMutable = NodeFactory.createMutableSimpleNode(
                new QName(needle.getNodeType(), "anySubNode"), null, "42", null, null);
        
        MutableCompositeNode newMutableCom = NodeFactory.createMutableCompositeNode(
                new QName(needle.getNodeType(), "anySubNode"), mutableParent, 
                NodeUtils.buildChildrenList(newMutable), null, null);
        NodeUtils.fixChildrenRelation(newMutableCom);
        newMutableCom.init();
        
        nodeModificationBuilder.addNode(newMutableCom);
        dumpResult();
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeDeleteComposite() throws Exception {
        LOG.debug("testBuildDiffTreeDeleteComposite");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<?> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));

        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        nodeModificationBuilder.deleteNode(mutableNeedle.getParent().asMutable());
        dumpResult();
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeDeleteSimple() throws Exception {
        LOG.debug("testBuildDiffTreeDeleteSimple");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<?> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));
        
        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        nodeModificationBuilder.deleteNode(mutableNeedle);
        dumpResult();
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeMerge() throws Exception {
        LOG.debug("testBuildDiffTreeMerge");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<?> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));
        
        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        mutableNeedle.setValue("tpId_18x");
        nodeModificationBuilder.mergeNode(mutableNeedle.getParent().asMutable());
        dumpResult();
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeRemoveComposite() throws Exception {
        LOG.debug("testBuildDiffTreeRemoveComposite");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<?> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));
        
        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        nodeModificationBuilder.removeNode(mutableNeedle.getParent().asMutable());
        dumpResult();
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeRemoveSimple() throws Exception {
        LOG.debug("testBuildDiffTreeRemoveSimple");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<?> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));
        
        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        nodeModificationBuilder.removeNode(mutableNeedle);
        dumpResult();
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeReplaceComposite() throws Exception {
        LOG.debug("testBuildDiffTreeReplaceComposite");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<?> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));
        
        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        mutableNeedle.setValue("tpId_18x");
        nodeModificationBuilder.replaceNode(mutableNeedle.getParent().asMutable());
        dumpResult();
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTreeReplaceSimple() throws Exception {
        LOG.debug("testBuildDiffTreeReplaceSimple");
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<?> needle = NodeUtils.findNodeByXpath(networkShadow, 
                NodeHelper.AddNamespaceToPattern(
                "//{0}node[{0}node-id='nodeId_19']//{0}termination-point[2]/{0}tp-id", ns));
        
        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        mutableNeedle.setValue("tpId_18x");
        nodeModificationBuilder.replaceNode(mutableNeedle);
        dumpResult();
    }


}
