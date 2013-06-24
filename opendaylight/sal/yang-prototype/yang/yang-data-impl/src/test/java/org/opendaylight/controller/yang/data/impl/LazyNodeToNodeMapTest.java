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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.Node;

/**
 * @author michal.rehak
 *
 */
public class LazyNodeToNodeMapTest {
    
    private LazyNodeToNodeMap lazyN2N;
    private CompositeNode tree;

    /**
     * prepare test values
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        lazyN2N = new LazyNodeToNodeMap();
        
        QName qName = new QName(
                new URI("urn:ietf:params:xml:ns:netconf:base:1.0"), 
                new Date(42), "yang-data-impl-mutableTest");
        
        tree = NodeHelper.buildTestConfigTree(qName);
    }

    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.LazyNodeToNodeMap#getMutableEquivalent(org.opendaylight.controller.yang.data.api.Node)}.
     */
    @Test
    public void testGetMutableEquivalent() {
        MutableCompositeNode mutableTree = (MutableCompositeNode) lazyN2N.getMutableEquivalent(tree);
        
        Assert.assertNull(mutableTree.getParent());
        Assert.assertEquals(tree.getNodeType(), mutableTree.getNodeType());
        Assert.assertEquals(1, lazyN2N.getKeyNodes().size());
        
        Node<?> subNode = tree.getCompositesByName("topologies").iterator().next();
        Node<?> subMutant = lazyN2N.getMutableEquivalent(subNode);
        
        Assert.assertNotNull(subMutant.getParent());
        Assert.assertEquals(subNode.getNodeType(), subMutant.getNodeType());
        Assert.assertEquals(2, lazyN2N.getKeyNodes().size());
        
        Assert.assertEquals(mutableTree, subMutant.getParent());
        Assert.assertEquals(mutableTree.getChildren().size(), 1);
        Assert.assertEquals(mutableTree.getChildren().iterator().next(), subMutant);
    }

    /**
     * Test method for {@link org.opendaylight.controller.yang.data.impl.LazyNodeToNodeMap#getMutableRoot()}.
     */
    @Test
    public void testGetMutableRoot() {
        Node<?> subNode = tree.getCompositesByName("topologies").iterator().next();
        Node<?> subMutant = lazyN2N.getMutableEquivalent(subNode);
        
        Assert.assertNotNull(subMutant.getParent());
        Assert.assertEquals(subMutant.getParent(), lazyN2N.getMutableRoot());
    }

}
