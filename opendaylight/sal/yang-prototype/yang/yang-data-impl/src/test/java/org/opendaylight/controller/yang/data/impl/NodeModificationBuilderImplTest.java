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
import org.opendaylight.controller.yang.data.api.SimpleNode;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.w3c.dom.Document;

/**
 * @author michal.rehak
 * 
 */
public class NodeModificationBuilderImplTest {

    private SchemaContext schemaCtx;
    private QName qName;
    private CompositeNode network;
    private NodeModificationBuilderImpl nodeModificationBuilder;

    /**
     * prepare schemaContext
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        schemaCtx = NodeHelper.loadSchemaContext();

        qName = new QName(
                new URI("urn:opendaylight:controller:network"), 
                new Date(1369000800000L), "topos");
        network = NodeHelper.buildTestConfigTree(qName);
        
        nodeModificationBuilder = new NodeModificationBuilderImpl(network, schemaCtx);
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.yang.data.impl.NodeModificationBuilderImpl#buildDiffTree()}
     * .
     * @throws Exception 
     */
    @Test
    public void testBuildDiffTree() throws Exception {
        Document networkShadow = NodeUtils.buildShadowDomTree(network);
        SimpleNode<String> needle = NodeUtils.findNodeByXpath(networkShadow, 
                "//node[node-id='nodeId_19']//termination-point[2]/tp-id");
        
        @SuppressWarnings("unchecked")
        MutableSimpleNode<String> mutableNeedle = (MutableSimpleNode<String>) 
                nodeModificationBuilder.getMutableEquivalent(needle);
        
        mutableNeedle.setValue("tpId_18x");
        nodeModificationBuilder.replaceNode(mutableNeedle);
        CompositeNode diffTree = nodeModificationBuilder.buildDiffTree();
        
        Document diffShadow = NodeUtils.buildShadowDomTree(diffTree);
        NodeHelper.dumpDoc(diffShadow, System.out);
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
        CompositeNode topologiesMutable = rootMutable.getCompositesByName("topologies").iterator().next();
        
        Assert.assertSame(topologiesMutable, nodeModificationBuilder.getMutableEquivalent(topologies));
    }

}
