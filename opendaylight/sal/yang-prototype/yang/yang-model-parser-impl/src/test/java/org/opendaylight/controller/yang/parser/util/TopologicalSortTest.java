/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.Node;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.NodeImpl;

import com.google.common.collect.Sets;

public class TopologicalSortTest {

    @Test(expected = IllegalStateException.class)
    public void test() throws Exception {
        Set<Node> nodes = Sets.newHashSet();

        NodeImpl node1 = new NodeImpl();
        nodes.add(node1);
        NodeImpl node2 = new NodeImpl();
        nodes.add(node2);
        NodeImpl node3 = new NodeImpl();
        nodes.add(node3);

        node1.addEdge(node2);
        node2.addEdge(node3);
        node3.addEdge(node1);

        try {
            TopologicalSort.sort(nodes);
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Test
    public void testValidSimple() throws Exception {
        Set<Node> nodes = Sets.newHashSet();

        Node node1 = new NodeImpl();
        nodes.add(node1);
        Node node2 = new NodeImpl();
        nodes.add(node2);
        Node node3 = new NodeImpl();
        nodes.add(node3);
        Node node4 = new NodeImpl();
        nodes.add(node4);

        ((NodeImpl) node1).addEdge(node2);
        ((NodeImpl) node1).addEdge(node3);
        ((NodeImpl) node2).addEdge(node4);
        ((NodeImpl) node3).addEdge(node2);

        List<Node> sorted = TopologicalSort.sort(nodes);

        assertThat(sorted.get(0), is(node4));
        assertThat(sorted.get(1), is(node2));
        assertThat(sorted.get(2), is(node3));
        assertThat(sorted.get(3), is(node1));
    }

}
